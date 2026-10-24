package com.b919.cave_dweller_legacy.entity.ai.path;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class DwellerPathFinder {
    private static final int MAX_EXPANSIONS = 800;
    private static final int WORLD_HEIGHT = 256;
    private static final EnumFacing[] HORIZONTAL_DIRECTIONS = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };

    public Path findPath(IBlockAccess world, BlockPos startPos, BlockPos targetPos, float maxDistance) {
        DwellerNodeType startType = getNodeType(world, startPos.getX(), startPos.getY(), startPos.getZ());
        if (startType == null) {
            return null;
        }
        if (getTerrainCost(world, startPos.getX(), startPos.getY(), startPos.getZ(), startType) == null) {
            return null;
        }

        Node start = new Node(startPos.getX(), startPos.getY(), startPos.getZ(), startType, false);
        start.heuristic = distance(start, targetPos);
        start.score = start.heuristic;

        Map<NodeKey, Node> nodes = new HashMap<>();
        PriorityQueue<QueueEntry> openSet = new PriorityQueue<>(Comparator.comparingDouble(entry -> entry.score));
        nodes.put(start.key, start);
        openSet.add(new QueueEntry(start, start.score));

        Node closest = start;
        Node reached = null;
        int expanded = 0;
        double maxDistanceSquared = maxDistance * maxDistance;

        while (!openSet.isEmpty() && expanded < MAX_EXPANSIONS) {
            QueueEntry entry = openSet.poll();
            Node current = entry.node;
            if (entry.score > current.score + 0.0001D || current.closed) {
                continue;
            }
            current.closed = true;
            expanded++;

            if (current.x == targetPos.getX() && current.y == targetPos.getY() && current.z == targetPos.getZ()) {
                reached = current;
                break;
            }
            if (current.heuristic < closest.heuristic) {
                closest = current;
            }

            if (distanceSquared(start, current) > maxDistanceSquared) {
                continue;
            }

            for (EnumFacing direction : HORIZONTAL_DIRECTIONS) {
                for (int verticalOffset = -1; verticalOffset <= 1; verticalOffset++) {
                    int x = current.x + direction.getFrontOffsetX();
                    int y = current.y + verticalOffset;
                    int z = current.z + direction.getFrontOffsetZ();
                    addNeighbor(world, targetPos, start, current, x, y, z, false, true, maxDistanceSquared, nodes, openSet);
                }
            }

            if (ModConfig.behavior.canClimb && hasClimbableWall(world, current.x, current.y, current.z)) {
                addNeighbor(world, targetPos, start, current, current.x, current.y + 1, current.z, true, false,
                        maxDistanceSquared, nodes, openSet);
                addNeighbor(world, targetPos, start, current, current.x, current.y - 1, current.z, true, false,
                        maxDistanceSquared, nodes, openSet);
            }
        }

        Node end = reached != null ? reached : closest;
        if (end == start) {
            return null;
        }

        List<PathPoint> points = new ArrayList<>();
        for (Node node = end; node != null; node = node.previous) {
            points.add(new DwellerPathPoint(node.x, node.y, node.z, node.nodeType, node.climbing));
        }
        Collections.reverse(points);
        return new Path(points.toArray(new PathPoint[0]));
    }

    private void addNeighbor(IBlockAccess world, BlockPos target, Node start, Node current, int x, int y, int z,
                             boolean climbing, boolean requireSupport, double maxDistanceSquared,
                             Map<NodeKey, Node> nodes, PriorityQueue<QueueEntry> openSet) {
        if (y < 0 || y >= WORLD_HEIGHT) {
            return;
        }

        DwellerNodeType nodeType = getNodeType(world, x, y, z);
        if (nodeType == null || requireSupport && !isSupported(world, x, y, z) && !isInWater(world, x, y, z)) {
            return;
        }
        if (climbing && !hasClimbableWall(world, x, y, z)
                && !hasClimbableWall(world, current.x, current.y, current.z)) {
            return;
        }

        Node candidate = new Node(x, y, z, nodeType, climbing);
        if (distanceSquared(start, candidate) > maxDistanceSquared) {
            return;
        }

        TerrainCost terrainCost = getTerrainCost(world, x, y, z, nodeType);
        if (terrainCost == null) {
            return;
        }

        float movementCost = climbing ? 1.8F : nodeType.getMovementCost();
        float stepCost = movementCost + terrainCost.cost;
        double tentativeCost = current.pathCost + stepCost;
        NodeKey key = candidate.key;
        Node known = nodes.get(key);
        if (known == null) {
            known = candidate;
            nodes.put(key, known);
        } else if (known.closed || tentativeCost >= known.pathCost) {
            return;
        }

        known.previous = current;
        known.pathCost = tentativeCost;
        known.heuristic = distance(known, target);
        known.score = known.pathCost + known.heuristic;
        openSet.add(new QueueEntry(known, known.score));
    }

    private DwellerNodeType getNodeType(IBlockAccess world, int x, int y, int z) {
        if (isClear(world, x, y, z, DwellerNodeType.WALK.getHeight())) {
            return DwellerNodeType.WALK;
        }
        if (ModConfig.behavior.canCrawl && isClear(world, x, y, z, DwellerNodeType.CROUCH.getHeight())) {
            return DwellerNodeType.CROUCH;
        }
        if (ModConfig.behavior.canCrawl && isClear(world, x, y, z, DwellerNodeType.CRAWL.getHeight())) {
            return DwellerNodeType.CRAWL;
        }
        return null;
    }

    private boolean isClear(IBlockAccess world, int x, int y, int z, float height) {
        AxisAlignedBB body = new AxisAlignedBB(x + 0.2D, y + 0.001D, z + 0.2D,
                x + 0.8D, y + height, z + 0.8D);
        int minX = MathHelper.floor(body.minX);
        int minY = MathHelper.floor(body.minY);
        int minZ = MathHelper.floor(body.minZ);
        int maxX = MathHelper.floor(body.maxX - 1.0E-7D);
        int maxY = MathHelper.floor(body.maxY - 1.0E-7D);
        int maxZ = MathHelper.floor(body.maxZ - 1.0E-7D);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                    IBlockState state = world.getBlockState(pos);
                    if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) {
                        return false;
                    }
                    AxisAlignedBB collisionBox = state.getCollisionBoundingBox(world, pos);
                    if (collisionBox != null && collisionBox != Block.NULL_AABB && body.intersects(collisionBox.offset(pos))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private TerrainCost getTerrainCost(IBlockAccess world, int x, int y, int z, DwellerNodeType nodeType) {
        AxisAlignedBB body = new AxisAlignedBB(x + 0.2D, y + 0.001D, z + 0.2D,
                x + 0.8D, y + nodeType.getHeight(), z + 0.8D);
        boolean water = false;
        boolean fire = false;
        int minX = MathHelper.floor(body.minX);
        int minY = MathHelper.floor(body.minY);
        int minZ = MathHelper.floor(body.minZ);
        int maxX = MathHelper.floor(body.maxX - 1.0E-7D);
        int maxY = MathHelper.floor(body.maxY - 1.0E-7D);
        int maxZ = MathHelper.floor(body.maxZ - 1.0E-7D);

        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    IBlockState state = world.getBlockState(new BlockPos(blockX, blockY, blockZ));
                    Material material = state.getMaterial();
                    if (material == Material.LAVA) {
                        return null;
                    }
                    water |= material == Material.WATER;
                    fire |= material == Material.FIRE;
                }
            }
        }
        return new TerrainCost((water ? 4.0F : 0.0F) + (fire ? 12.0F : 0.0F));
    }

    private boolean isSupported(IBlockAccess world, int x, int y, int z) {
        BlockPos supportPos = new BlockPos(x, y - 1, z);
        IBlockState state = world.getBlockState(supportPos);
        return state.getBlock().isSideSolid(state, world, supportPos, EnumFacing.UP);
    }

    private boolean isInWater(IBlockAccess world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getMaterial() == Material.WATER;
    }

    private boolean hasClimbableWall(IBlockAccess world, int x, int y, int z) {
        for (EnumFacing direction : HORIZONTAL_DIRECTIONS) {
            BlockPos wallPos = new BlockPos(x + direction.getFrontOffsetX(), y, z + direction.getFrontOffsetZ());
            IBlockState state = world.getBlockState(wallPos);
            if (state.getMaterial().isSolid() && state.getCollisionBoundingBox(world, wallPos) != null
                    && state.getCollisionBoundingBox(world, wallPos) != Block.NULL_AABB) {
                return true;
            }
        }
        return false;
    }

    private double distance(Node node, BlockPos target) {
        double dx = node.x - target.getX();
        double dy = node.y - target.getY();
        double dz = node.z - target.getZ();
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    }

    private double distanceSquared(Node first, Node second) {
        double dx = first.x - second.x;
        double dy = first.y - second.y;
        double dz = first.z - second.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static class TerrainCost {
        private final float cost;

        private TerrainCost(float cost) {
            this.cost = cost;
        }
    }

    private static class Node {
        private final int x;
        private final int y;
        private final int z;
        private final DwellerNodeType nodeType;
        private final boolean climbing;
        private final NodeKey key;
        private Node previous;
        private double pathCost;
        private double heuristic;
        private double score;
        private boolean closed;

        private Node(int x, int y, int z, DwellerNodeType nodeType, boolean climbing) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nodeType = nodeType;
            this.climbing = climbing;
            this.key = new NodeKey(x, y, z, nodeType, climbing);
        }
    }

    private static class NodeKey {
        private final int x;
        private final int y;
        private final int z;
        private final DwellerNodeType nodeType;
        private final boolean climbing;

        private NodeKey(int x, int y, int z, DwellerNodeType nodeType, boolean climbing) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nodeType = nodeType;
            this.climbing = climbing;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof NodeKey)) return false;
            NodeKey other = (NodeKey) object;
            return x == other.x && y == other.y && z == other.z && climbing == other.climbing && nodeType == other.nodeType;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            result = 31 * result + nodeType.hashCode();
            result = 31 * result + (climbing ? 1 : 0);
            return result;
        }
    }

    private static class QueueEntry {
        private final Node node;
        private final double score;

        private QueueEntry(Node node, double score) {
            this.node = node;
            this.score = score;
        }
    }
}