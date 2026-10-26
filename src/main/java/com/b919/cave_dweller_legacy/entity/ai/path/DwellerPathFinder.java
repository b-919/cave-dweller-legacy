package com.b919.cave_dweller_legacy.entity.ai.path;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.util.DwellerLeap;
import com.b919.cave_dweller_legacy.util.DwellerLiquid;
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
    private static final int BASE_EXPANSIONS = 800;
    private static final int HARD_MAX_EXPANSIONS = 4000;
    private static final int WORLD_HEIGHT = 256;
    private static final int LEAP_SIMULATION_BUDGET = 768;
    private static final int LEAP_CHECK_BUDGET = 8192;
    private static final EnumFacing[] HORIZONTAL_DIRECTIONS = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };
    private static final int[][] LEAP_DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private int leapSimulations;
    private int leapChecks;

    public Path findPath(IBlockAccess world, BlockPos startPos, BlockPos targetPos, float maxDistance) {
        this.leapSimulations = 0;
        this.leapChecks = 0;
        DwellerNodeType startType = getNodeType(world, startPos.getX(), startPos.getY(), startPos.getZ(), true);
        if (startType == null) {
            return null;
        }
        if (getTerrainCost(world, startPos.getX(), startPos.getY(), startPos.getZ(), startType, true) == null) {
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
        double targetDistance = distance(start, targetPos);
        int maxExpansions = Math.min(HARD_MAX_EXPANSIONS,
                BASE_EXPANSIONS + (int) (targetDistance * targetDistance * 1.5D));

        while (!openSet.isEmpty() && expanded < maxExpansions) {
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

            if (ModConfig.swimming.canSwim && isSwimLiquid(world, current.x, current.y, current.z)) {
                addNeighbor(world, targetPos, start, current, current.x, current.y + 1, current.z, false, false,
                        maxDistanceSquared, nodes, openSet);
                addNeighbor(world, targetPos, start, current, current.x, current.y - 1, current.z, false, false,
                        maxDistanceSquared, nodes, openSet);
            }

            if (ModConfig.leaping.enabled && current.nodeType == DwellerNodeType.WALK) {
                addLeapNeighbors(world, targetPos, start, current, maxDistanceSquared, nodes, openSet);
            }
        }

        Node end = reached != null ? reached : closest;
        if (end == start) {
            return null;
        }

        List<PathPoint> points = new ArrayList<>();
        for (Node node = end; node != null; node = node.previous) {
            points.add(new DwellerPathPoint(node.x, node.y, node.z, node.nodeType,
                    node.climbing, node.leap));
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
        if (nodeType == null || requireSupport && !isSupported(world, x, y, z) && !isSwimLiquid(world, x, y, z)) {
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

    private void addLeapNeighbors(IBlockAccess world, BlockPos target, Node start, Node current,
                                  double maxDistanceSquared, Map<NodeKey, Node> nodes,
                                  PriorityQueue<QueueEntry> openSet) {
        double maxLeapDistance = ModConfig.leaping.maxLeapDistance;
        if (maxLeapDistance < 1.0D) {
            return;
        }
        int maxRise = Math.max(0, ModConfig.leaping.maxLeapRise);
        int maxDrop = Math.max(0, ModConfig.leaping.maxLeapDrop);
        double speed = Math.max(0.05D, ModConfig.leaping.leapSpeed);
        int maxTicks = Math.max(1, ModConfig.leaping.maxLeapTicks);

        if (!isSupported(world, current.x, current.y, current.z)
                || bodyContainsLiquid(world, current.x, current.y, current.z,
                DwellerNodeType.WALK.getHeight())) {
            return;
        }

        for (int[] direction : LEAP_DIRECTIONS) {
            int dirX = direction[0];
            int dirZ = direction[1];
            if (!isLeapRequired(world, current, dirX, dirZ)) {
                continue;
            }

            double stepLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
            int maxStep = MathHelper.floor(maxLeapDistance / stepLength + 1.0E-4D);
            boolean added = false;
            for (int step = 2; step <= maxStep && !added; step++) {
                if (this.leapSimulations >= LEAP_SIMULATION_BUDGET
                        || this.leapChecks >= LEAP_CHECK_BUDGET) {
                    return;
                }

                int x = current.x + dirX * step;
                int z = current.z + dirZ * step;
                double distance = step * stepLength;
                for (int magnitude = 0; magnitude <= Math.max(maxRise, maxDrop) && !added; magnitude++) {
                    if (magnitude == 0) {
                        added = tryLeap(world, target, start, current, x, current.y, z, distance,
                                speed, maxTicks, maxRise, maxDrop, maxDistanceSquared, nodes, openSet);
                    } else {
                        if (magnitude <= maxRise) {
                            added = tryLeap(world, target, start, current, x, current.y + magnitude, z,
                                    distance, speed, maxTicks, maxRise, maxDrop, maxDistanceSquared,
                                    nodes, openSet);
                        }
                        if (!added && magnitude <= maxDrop) {
                            added = tryLeap(world, target, start, current, x, current.y - magnitude, z,
                                    distance, speed, maxTicks, maxRise, maxDrop, maxDistanceSquared,
                                    nodes, openSet);
                        }
                    }
                }
            }
        }
    }

    private boolean tryLeap(IBlockAccess world, BlockPos target, Node start, Node current,
                            int x, int y, int z, double distance, double speed, int maxTicks,
                            int maxRise, int maxDrop, double maxDistanceSquared,
                            Map<NodeKey, Node> nodes, PriorityQueue<QueueEntry> openSet) {
        if (this.leapChecks >= LEAP_CHECK_BUDGET) {
            return false;
        }
        this.leapChecks++;
        if (y < 0 || y >= WORLD_HEIGHT) {
            return false;
        }

        DwellerNodeType nodeType = getNodeType(world, x, y, z);
        if (nodeType != DwellerNodeType.WALK || !isSupported(world, x, y, z)) {
            return false;
        }

        TerrainCost terrainCost = getTerrainCost(world, x, y, z, nodeType);
        if (terrainCost == null) {
            return false;
        }

        this.leapSimulations++;
        if (!DwellerLeap.canTraverse(world, current.x, current.y, current.z, x, y, z,
                speed, maxTicks, maxRise, maxDrop)) {
            return false;
        }

        Node candidate = new Node(x, y, z, nodeType, false, true);
        if (distanceSquared(start, candidate) > maxDistanceSquared) {
            return false;
        }

        float stepCost = nodeType.getMovementCost() + terrainCost.cost
                + (float) ModConfig.leaping.leapPathCost + (float) distance;
        double tentativeCost = current.pathCost + stepCost;
        NodeKey key = candidate.key;
        Node known = nodes.get(key);
        if (known == null) {
            known = candidate;
            nodes.put(key, known);
        } else if (known.closed || tentativeCost >= known.pathCost) {
            return false;
        }

        known.previous = current;
        known.pathCost = tentativeCost;
        known.heuristic = distance(known, target);
        known.score = known.pathCost + known.heuristic;
        openSet.add(new QueueEntry(known, known.score));
        return true;
    }

    private boolean isLeapRequired(IBlockAccess world, Node current, int dirX, int dirZ) {
        int x = current.x + dirX;
        int z = current.z + dirZ;
        for (int offset = -1; offset <= 1; offset++) {
            int y = current.y + offset;
            if (y < 0 || y >= WORLD_HEIGHT) {
                continue;
            }
            if (getNodeType(world, x, y, z) != null
                    && (isSupported(world, x, y, z) || isSwimLiquid(world, x, y, z))) {
                return false;
            }
        }
        return true;
    }

    private boolean bodyContainsLiquid(IBlockAccess world, int x, int y, int z, float height) {
        int top = y + MathHelper.ceil(height) - 1;
        for (int blockY = y; blockY <= top; blockY++) {
            if (DwellerLiquid.get(world, new BlockPos(x, blockY, z)) != null) {
                return true;
            }
        }
        return false;
    }

    private DwellerNodeType getNodeType(IBlockAccess world, int x, int y, int z) {
        return getNodeType(world, x, y, z, false);
    }

    private DwellerNodeType getNodeType(IBlockAccess world, int x, int y, int z, boolean allowLava) {
        if (isClear(world, x, y, z, DwellerNodeType.WALK.getHeight(), allowLava)) {
            return DwellerNodeType.WALK;
        }
        if (ModConfig.behavior.canCrawl
                && isClear(world, x, y, z, DwellerNodeType.CROUCH.getHeight(), allowLava)) {
            return DwellerNodeType.CROUCH;
        }
        if (ModConfig.behavior.canCrawl
                && isClear(world, x, y, z, DwellerNodeType.CRAWL.getHeight(), allowLava)) {
            return DwellerNodeType.CRAWL;
        }
        return null;
    }

    private boolean isClear(IBlockAccess world, int x, int y, int z, float height) {
        return isClear(world, x, y, z, height, false);
    }

    private boolean isClear(IBlockAccess world, int x, int y, int z, float height, boolean allowLava) {
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
                    if (!allowLava && (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA)) {
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
        return getTerrainCost(world, x, y, z, nodeType, false);
    }

    private TerrainCost getTerrainCost(IBlockAccess world, int x, int y, int z, DwellerNodeType nodeType,
                                       boolean allowLava) {
        AxisAlignedBB body = new AxisAlignedBB(x + 0.2D, y + 0.001D, z + 0.2D,
                x + 0.8D, y + nodeType.getHeight(), z + 0.8D);
        DwellerLiquid.LiquidInfo moddedLiquid = null;
        boolean water = false;
        boolean lava = false;
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
                    BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                    DwellerLiquid.LiquidInfo liquid = DwellerLiquid.get(world, pos);
                    if (liquid != null) {
                        if (liquid.isLava()) {
                            if (!allowLava) {
                                return null;
                            }
                            lava = true;
                        } else if (liquid.isModded()) {
                            moddedLiquid = liquid;
                        } else {
                            water = true;
                        }
                        continue;
                    }
                    fire |= world.getBlockState(pos).getMaterial() == Material.FIRE;
                }
            }
        }

        BlockPos headPos = new BlockPos(x, y + MathHelper.ceil(nodeType.getHeight()) - 1, z);
        DwellerLiquid.LiquidInfo headLiquid = DwellerLiquid.get(world, headPos);
        float waterCost = 0.0F;
        if (water) {
            waterCost = (float) ModConfig.swimming.waterSurfaceCost;
            if (ModConfig.swimming.canSwim && headLiquid != null && headLiquid.isWater()) {
                waterCost = (float) ModConfig.swimming.waterSubmergedCost;
            }
        }
        float moddedCost = 0.0F;
        if (moddedLiquid != null) {
            moddedCost = moddedLiquid.getSurfaceCost();
            if (ModConfig.swimming.canSwim && headLiquid == moddedLiquid) {
                moddedCost = moddedLiquid.getSubmergedCost();
            }
        }
        return new TerrainCost((lava ? 16.0F : 0.0F) + waterCost + moddedCost + (fire ? 12.0F : 0.0F));
    }

    private boolean isSupported(IBlockAccess world, int x, int y, int z) {
        BlockPos supportPos = new BlockPos(x, y - 1, z);
        IBlockState state = world.getBlockState(supportPos);
        return state.getBlock().isSideSolid(state, world, supportPos, EnumFacing.UP);
    }

    private boolean isSwimLiquid(IBlockAccess world, int x, int y, int z) {
        DwellerLiquid.LiquidInfo liquid = DwellerLiquid.get(world, new BlockPos(x, y, z));
        return liquid != null && !liquid.isLava();
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
        private final boolean leap;
        private final NodeKey key;
        private Node previous;
        private double pathCost;
        private double heuristic;
        private double score;
        private boolean closed;

        private Node(int x, int y, int z, DwellerNodeType nodeType, boolean climbing) {
            this(x, y, z, nodeType, climbing, false);
        }

        private Node(int x, int y, int z, DwellerNodeType nodeType, boolean climbing, boolean leap) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nodeType = nodeType;
            this.climbing = climbing;
            this.leap = leap;
            this.key = new NodeKey(x, y, z, nodeType, climbing, leap);
        }
    }

    private static class NodeKey {
        private final int x;
        private final int y;
        private final int z;
        private final DwellerNodeType nodeType;
        private final boolean climbing;
        private final boolean leap;

        private NodeKey(int x, int y, int z, DwellerNodeType nodeType, boolean climbing, boolean leap) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nodeType = nodeType;
            this.climbing = climbing;
            this.leap = leap;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof NodeKey)) return false;
            NodeKey other = (NodeKey) object;
            return x == other.x && y == other.y && z == other.z && climbing == other.climbing
                    && leap == other.leap && nodeType == other.nodeType;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            result = 31 * result + nodeType.hashCode();
            result = 31 * result + (climbing ? 1 : 0);
            result = 31 * result + (leap ? 1 : 0);
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