package com.b919.cave_dweller_legacy.entity.ai.path;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PathNavigateDweller extends PathNavigateGround {
    private static final long REPATH_INTERVAL = 10L;
    private static final double CLIMB_SPEED = 0.3D;
    private static final EnumFacing[] HORIZONTAL_DIRECTIONS = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };

    private final CaveDwellerEntity dweller;
    private final DwellerPathFinder dwellerPathFinder;
    private BlockPos cachedTarget;
    private long lastPathTime = Long.MIN_VALUE;
    private boolean climbing;

    public PathNavigateDweller(CaveDwellerEntity entity, World world) {
        super(entity, world);
        this.dweller = entity;
        this.dwellerPathFinder = new DwellerPathFinder();
    }

    @Override
    public void onUpdateNavigation() {
        DwellerNodeType nodeType = getDesiredNodeType();
        if (nodeType != null) {
            this.dweller.applyNavigationPosture(nodeType);
        }
        super.onUpdateNavigation();
        updateClimbing();
    }

    /**
     * Vanilla path following cannot drive purely vertical waypoints: the move helper is handed a
     * target one block below the waypoint and refuses to move when the horizontal delta is zero.
     * Climbing is therefore driven manually here.
     */
    private void updateClimbing() {
        boolean wasClimbing = this.climbing;
        this.climbing = false;
        if (!ModConfig.behavior.canClimb || this.currentPath == null || this.currentPath.isFinished()) {
            return;
        }

        int index = this.currentPath.getCurrentPathIndex();
        DwellerPathPoint point = getDwellerPoint(index);
        double feetY = this.entity.getEntityBoundingBox().minY;
        boolean wantsToGoUp = point.y > feetY + 0.35D;
        if (!wantsToGoUp) {
            if (point.isClimbing() && Math.abs(point.y - feetY) < 0.75D) {
                nudgeOntoLedge(index);
            }
            return;
        }
        if (!point.isClimbing() && !isVerticalStep(index)) {
            return;
        }

        EnumFacing wall = findClimbableWall();
        if (wall == null) {
            return;
        }

        if (!wasClimbing && ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] Climbing wall " + wall + " towards " + point.y);
        }
        this.climbing = true;
        this.dweller.fallDistance = 0.0F;
        this.dweller.motionY = Math.max(this.dweller.motionY, CLIMB_SPEED);

        // Hug the wall so the dweller keeps contact while ascending.
        this.dweller.motionX += wall.getFrontOffsetX() * 0.05D;
        this.dweller.motionZ += wall.getFrontOffsetZ() * 0.05D;

        Vec3d waypoint = this.currentPath.getVectorFromIndex(this.entity, index);
        this.entity.getMoveHelper().setMoveTo(waypoint.x, point.y, waypoint.z, this.speed);
    }

    private boolean isVerticalStep(int index) {
        if (index <= 0) {
            return false;
        }
        assert this.currentPath != null;
        PathPoint previous = this.currentPath.getPathPointFromIndex(index - 1);
        PathPoint current = this.currentPath.getPathPointFromIndex(index);
        return previous.x == current.x && previous.z == current.z && current.y > previous.y;
    }

    private void nudgeOntoLedge(int index) {
        if (this.currentPath == null || index + 1 >= this.currentPath.getCurrentPathLength()) {
            return;
        }
        PathPoint next = this.currentPath.getPathPointFromIndex(index + 1);
        double dx = next.x + 0.5D - this.entity.posX;
        double dz = next.z + 0.5D - this.entity.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            return;
        }
        this.dweller.motionX += dx / length * 0.04D;
        this.dweller.motionZ += dz / length * 0.04D;
    }

    @Nullable
    private EnumFacing findClimbableWall() {
        BlockPos feet = new BlockPos(this.entity.posX, this.entity.getEntityBoundingBox().minY + 0.1D, this.entity.posZ);
        for (int offsetY = 0; offsetY <= 1; offsetY++) {
            for (EnumFacing direction : HORIZONTAL_DIRECTIONS) {
                BlockPos wallPos = feet.up(offsetY).offset(direction);
                IBlockState state = this.world.getBlockState(wallPos);
                if (state.getMaterial().isSolid()
                        && state.getCollisionBoundingBox(this.world, wallPos) != Block.NULL_AABB
                        && state.getCollisionBoundingBox(this.world, wallPos) != null) {
                    return direction;
                }
            }
        }
        return null;
    }

    @Override @MethodsReturnNonnullByDefault
    public Path getPathToPos(@Nonnull BlockPos targetPos) {
        return findDwellerPath(targetPos);
    }

    @Override @MethodsReturnNonnullByDefault
    public Path getPathToEntityLiving(Entity target) {
        return findDwellerPath(new BlockPos(target.posX, target.getEntityBoundingBox().minY, target.posZ));
    }

    private Path findDwellerPath(BlockPos targetPos) {
        long worldTime = this.world.getTotalWorldTime();
        if (this.currentPath != null && !this.currentPath.isFinished() && targetPos.equals(this.cachedTarget)
                && worldTime - this.lastPathTime < REPATH_INTERVAL) {
            return this.currentPath;
        }

        this.climbing = false;
        this.cachedTarget = targetPos.toImmutable();
        this.lastPathTime = worldTime;
        float maxDistance = this.getPathSearchRange();
        int radius = MathHelper.ceil(maxDistance) + 2;
        BlockPos startPos = new BlockPos(this.entity.posX, this.entity.getEntityBoundingBox().minY, this.entity.posZ);
        ChunkCache cache = new ChunkCache(this.world, startPos.add(-radius, -radius, -radius),
                startPos.add(radius, radius, radius), 0);
        this.world.profiler.startSection("dweller_pathfind");
        Path path = this.dwellerPathFinder.findPath(cache, startPos, targetPos, maxDistance);
        this.world.profiler.endSection();
        return path;
    }

    @Override
    protected boolean canNavigate() {
        return super.canNavigate() || !this.noPath();
    }

    @Override
    protected boolean isDirectPathBetweenPoints(@Nonnull Vec3d from, @Nonnull Vec3d to, int sizeX, int sizeY, int sizeZ) {
        return false;
    }

    @Nullable
    public DwellerNodeType getDesiredNodeType() {
        if (this.currentPath == null || this.currentPath.isFinished()) {
            return null;
        }

        int index = this.currentPath.getCurrentPathIndex();
        DwellerPathPoint current = getDwellerPoint(index);
        DwellerPathPoint next = index + 1 < this.currentPath.getCurrentPathLength() ? getDwellerPoint(index + 1) : null;
        if (next != null && next.getNodeType().getHeight() < current.getNodeType().getHeight()) {
            return next.getNodeType();
        }
        return current.getNodeType();
    }

    public boolean isClimbingUp() {
        return this.climbing;
    }

    public boolean canReachPosition(BlockPos pos) {
        Path path = this.getPathToPos(pos);
        if (path.getCurrentPathLength() == 0) {
            return false;
        }
        PathPoint end = path.getPathPointFromIndex(path.getCurrentPathLength() - 1);
        double dx = end.x + 0.5D - (pos.getX() + 0.5D);
        double dy = end.y - pos.getY();
        double dz = end.z + 0.5D - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= 2.25D;
    }

    public void onBlockChanged(BlockPos changedPos) {
        if (this.currentPath == null || this.currentPath.isFinished()) {
            return;
        }

        double dx = this.entity.posX - (changedPos.getX() + 0.5D);
        double dy = this.entity.posY - (changedPos.getY() + 0.5D);
        double dz = this.entity.posZ - (changedPos.getZ() + 0.5D);
        double range = this.getPathSearchRange();
        if (dx * dx + dy * dy + dz * dz <= range * range) {
            this.clearPath();
        }
    }

    private DwellerPathPoint getDwellerPoint(int index) {
        assert this.currentPath != null;
        PathPoint point = this.currentPath.getPathPointFromIndex(index);
        if (point instanceof DwellerPathPoint) {
            return (DwellerPathPoint) point;
        }
        return new DwellerPathPoint(point.x, point.y, point.z, DwellerNodeType.WALK, false);
    }
}