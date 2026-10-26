package com.b919.cave_dweller_legacy.entity.ai.path;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.util.DwellerLeap;
import com.b919.cave_dweller_legacy.util.DwellerLiquid;
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
    private static final double LEAP_START_RANGE = 1.0D;
    private static final double LEAP_START_VERTICAL = 1.0D;
    private static final double LEAP_ARRIVAL_RANGE = 0.35D;
    private static final EnumFacing[] HORIZONTAL_DIRECTIONS = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };

    private final CaveDwellerEntity dweller;
    private final DwellerPathFinder dwellerPathFinder;
    private BlockPos cachedTarget;
    private long lastPathTime = Long.MIN_VALUE;
    private boolean climbing;
    private boolean swimming;
    private boolean leaping;
    private int leapTicks;
    private int leapTicksMax;
    private double leapSpeed;
    private double leapTargetX;
    private double leapTargetZ;
    private int leapTargetIndex = -1;
    private int leapCooldownTicks;

    public PathNavigateDweller(CaveDwellerEntity entity, World world) {
        super(entity, world);
        this.dweller = entity;
        this.dwellerPathFinder = new DwellerPathFinder();
    }

    @Override
    public void onUpdateNavigation() {
        if (this.leapCooldownTicks > 0) {
            this.leapCooldownTicks--;
        }

        if (this.leaping) {
            this.dweller.applyNavigationPosture(DwellerNodeType.WALK);
            updateLeap();
            updateSwimming();
            updateClimbing();
            return;
        }

        DwellerNodeType nodeType = getDesiredNodeType();
        if (nodeType != null) {
            this.dweller.applyNavigationPosture(nodeType);
        }
        super.onUpdateNavigation();

        if (this.leapCooldownTicks > 0 && this.dweller.onGround
                && DwellerLiquid.findInEntity(this.world, this.dweller.getEntityBoundingBox()) == null
                && findPendingLeap() != null) {
            holdPosition();
        } else {
            tryStartLeap();
        }
        updateSwimming();
        updateClimbing();
    }
    private void updateSwimming() {
        if (this.leaping) {
            this.swimming = false;
            return;
        }
        DwellerLiquid.LiquidInfo liquid = DwellerLiquid.findInEntity(
                this.world, this.dweller.getEntityBoundingBox());
        if (!ModConfig.swimming.canSwim || liquid == null) {
            this.swimming = false;
            return;
        }

        DwellerLiquid.LiquidInfo feetLiquid = DwellerLiquid.get(this.world, new BlockPos(
                this.dweller.posX, this.dweller.getEntityBoundingBox().minY, this.dweller.posZ));
        if (!this.swimming && this.dweller.onGround
                && (feetLiquid == null || !isDeepLiquid(feetLiquid,
                this.dweller.posX, this.dweller.posY, this.dweller.posZ))) {
            return;
        }
        this.swimming = true;

        this.dweller.motionY = getMotionY(liquid);
        this.dweller.fallDistance = 0.0F;
    }

    private double getMotionY(DwellerLiquid.LiquidInfo liquid) {
        double targetY = getSwimTargetY(liquid);
        double feetY = this.dweller.getEntityBoundingBox().minY;
        double motionY = getMotionY(liquid, targetY, feetY);

        if (this.dweller.collidedHorizontally && this.dweller.motionY > motionY) {
            motionY = Math.min(this.dweller.motionY, ModConfig.swimming.maxLedgeBoost);
        }
        return motionY;
    }

    private static double getMotionY(DwellerLiquid.LiquidInfo liquid, double targetY, double feetY) {
        double error = targetY - feetY;
        double maxUp = liquid.getUpSpeed();
        double maxDown = liquid.getDownSpeed();
        double motionY = MathHelper.clamp(error * ModConfig.swimming.verticalResponse, -maxDown, maxUp);
        if (Math.abs(error) < ModConfig.swimming.verticalDeadZone) {
            motionY = 0.0D;
        }

        return motionY;
    }

    private double getSwimTargetY(DwellerLiquid.LiquidInfo liquid) {
        if (this.currentPath != null && !this.currentPath.isFinished()) {
            return getDwellerPoint(this.currentPath.getCurrentPathIndex()).y;
        }
        return findLiquidSurfaceY(liquid);
    }

    private double findLiquidSurfaceY(DwellerLiquid.LiquidInfo liquid) {
        int x = MathHelper.floor(this.dweller.posX);
        int z = MathHelper.floor(this.dweller.posZ);
        int baseY = MathHelper.floor(this.dweller.getEntityBoundingBox().minY);
        int topY = Integer.MIN_VALUE;

        for (int y = baseY + 2; y >= baseY - 2; y--) {
            if (liquid.matches(this.world, new BlockPos(x, y, z))) {
                topY = y;
                break;
            }
        }
        if (topY == Integer.MIN_VALUE) {
            return this.dweller.getEntityBoundingBox().minY;
        }
        for (int y = topY + 1; y < 255; y++) {
            if (!liquid.matches(this.world, new BlockPos(x, y, z))) {
                break;
            }
            topY = y;
        }
        return topY + 1;
    }

    private boolean isDeepLiquid(DwellerLiquid.LiquidInfo liquid, double x, double y, double z) {
        for (int offset = 1; offset < ModConfig.swimming.minimumSwimDepth; offset++) {
            if (!liquid.matches(this.world, new BlockPos(x, y + offset, z))) {
                return false;
            }
        }
        return true;
    }

    private void updateClimbing() {
        boolean wasClimbing = this.climbing;
        this.climbing = false;
        if (this.leaping) {
            return;
        }
        if (!ModConfig.behavior.canClimb || this.currentPath == null || this.currentPath.isFinished()) {
            return;
        }

        int index = this.currentPath.getCurrentPathIndex();
        DwellerPathPoint point = getDwellerPoint(index);
        double feetY = this.entity.getEntityBoundingBox().minY;
        boolean ledgeAfterClimb = isLedgeAfterClimb(index);
        boolean wantsToGoUp = point.y > feetY + 0.35D;
        if (!wantsToGoUp) {
            if ((point.isClimbing() || ledgeAfterClimb) && Math.abs(point.y - feetY) < 0.75D) {
                nudgeOntoLedge(index);
            }
            return;
        }
        if (!point.isClimbing() && !ledgeAfterClimb && !isVerticalStep(index)) {
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

        this.dweller.motionX += wall.getFrontOffsetX() * 0.05D;
        this.dweller.motionZ += wall.getFrontOffsetZ() * 0.05D;

        Vec3d waypoint = this.currentPath.getVectorFromIndex(this.entity, index);
        this.entity.getMoveHelper().setMoveTo(waypoint.x, point.y, waypoint.z, this.speed);
    }

    private boolean isLedgeAfterClimb(int index) {
        if (index <= 0 || this.currentPath == null) {
            return false;
        }
        DwellerPathPoint previous = getDwellerPoint(index - 1);
        if (!previous.isClimbing()) {
            return false;
        }
        DwellerPathPoint current = getDwellerPoint(index);
        return (current.x != previous.x || current.z != previous.z)
                && current.y >= previous.y;
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
        if (this.currentPath == null) {
            return;
        }
        PathPoint target = index + 1 < this.currentPath.getCurrentPathLength()
                ? this.currentPath.getPathPointFromIndex(index + 1)
                : getDwellerPoint(index);
        double dx = target.x + 0.5D - this.entity.posX;
        double dz = target.z + 0.5D - this.entity.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            return;
        }
        this.dweller.motionX += dx / length * 0.04D;
        this.dweller.motionZ += dz / length * 0.04D;
    }

    private void tryStartLeap() {
        if (!ModConfig.leaping.enabled || this.leapCooldownTicks > 0) {
            return;
        }
        if (!this.dweller.onGround || !this.dweller.isEntityAlive()) {
            return;
        }
        if (DwellerLiquid.findInEntity(this.world, this.dweller.getEntityBoundingBox()) != null) {
            return;
        }

        DwellerPathPoint target = findPendingLeap();
        if (target != null) {
            startLeap(target);
        }
    }

    @Nullable
    private DwellerPathPoint findPendingLeap() {
        if (this.currentPath == null || this.currentPath.isFinished()) {
            return null;
        }

        this.leapTargetIndex = -1;
        int index = this.currentPath.getCurrentPathIndex();
        DwellerPathPoint target = getDwellerPoint(index);
        DwellerPathPoint source = null;
        int targetIndex = index;
        if (target.isLeap()) {
            if (index > 0) {
                source = getDwellerPoint(index - 1);
            }
        } else if (index + 1 < this.currentPath.getCurrentPathLength()) {
            DwellerPathPoint next = getDwellerPoint(index + 1);
            if (next.isLeap()) {
                source = target;
                target = next;
                targetIndex = index + 1;
            }
        }
        if (source == null) {
            return null;
        }

        double feetY = this.dweller.getEntityBoundingBox().minY;
        double offsetX = this.dweller.posX - (source.x + 0.5D);
        double offsetZ = this.dweller.posZ - (source.z + 0.5D);
        if (offsetX * offsetX + offsetZ * offsetZ > LEAP_START_RANGE * LEAP_START_RANGE
                || Math.abs(feetY - source.y) > LEAP_START_VERTICAL) {
            return null;
        }

        this.leapTargetIndex = targetIndex;
        return target;
    }

    private void holdPosition() {
        this.dweller.getMoveHelper().setMoveTo(this.dweller.posX, this.dweller.posY, this.dweller.posZ,
                this.speed);
        this.dweller.motionX = 0.0D;
        this.dweller.motionZ = 0.0D;
    }

    private void startLeap(DwellerPathPoint target) {
        double targetX = target.x + 0.5D;
        double targetZ = target.z + 0.5D;
        double deltaX = targetX - this.dweller.posX;
        double deltaZ = targetZ - this.dweller.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 0.25D) {
            return;
        }

        double configuredSpeed = Math.max(0.05D, ModConfig.leaping.leapSpeed);
        int maxTicks = Math.max(1, ModConfig.leaping.maxLeapTicks);
        int ticks = DwellerLeap.flightTicks(distance, configuredSpeed, maxTicks);
        double rise = target.y - this.dweller.getEntityBoundingBox().minY;

        this.leaping = true;
        this.leapTicks = 0;
        this.leapTicksMax = ticks;
        this.leapSpeed = DwellerLeap.horizontalSpeed(distance, ticks);
        this.leapTargetX = targetX;
        this.leapTargetZ = targetZ;

        if (this.currentPath != null && this.leapTargetIndex >= 0
                && this.leapTargetIndex < this.currentPath.getCurrentPathLength()) {
            this.currentPath.setCurrentPathIndex(this.leapTargetIndex);
        }

        this.dweller.getMoveHelper().setMoveTo(this.dweller.posX, this.dweller.posY, this.dweller.posZ,
                this.speed);
        this.dweller.motionY = DwellerLeap.launchMotionY(rise, ticks);
        this.dweller.fallDistance = 0.0F;
        applyLeapHorizontalMotion();

        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] Leaping " + String.format("%.2f", distance)
                    + " blocks, rise " + rise + ", " + ticks + " ticks");
        }
    }

    private void updateLeap() {
        this.leapTicks++;
        this.dweller.getMoveHelper().setMoveTo(this.dweller.posX, this.dweller.posY, this.dweller.posZ,
                this.speed);

        if (DwellerLiquid.findInEntity(this.world, this.dweller.getEntityBoundingBox()) != null) {
            finishLeap();
            return;
        }

        double deltaX = this.leapTargetX - this.dweller.posX;
        double deltaZ = this.leapTargetZ - this.dweller.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double arrivalRange = Math.min(LEAP_ARRIVAL_RANGE,
                Math.max(0.02D, this.leapSpeed * 0.5D));
        if (distance <= arrivalRange || this.leapTicks >= this.leapTicksMax) {
            finishLeap();
            return;
        }

        applyLeapHorizontalMotion();
        this.dweller.fallDistance = 0.0F;
    }

    private void applyLeapHorizontalMotion() {
        double deltaX = this.leapTargetX - this.dweller.posX;
        double deltaZ = this.leapTargetZ - this.dweller.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 1.0E-4D) {
            this.dweller.motionX = 0.0D;
            this.dweller.motionZ = 0.0D;
            return;
        }

        double speed = Math.min(this.leapSpeed, distance);
        this.dweller.setMoveForward(0.0F);
        this.dweller.setMoveStrafing(0.0F);
        this.dweller.motionX = deltaX / distance * speed;
        this.dweller.motionZ = deltaZ / distance * speed;
        float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
        this.dweller.rotationYaw = yaw;
        this.dweller.renderYawOffset = yaw;
    }

    private void finishLeap() {
        this.leaping = false;
        this.leapTicks = 0;
        this.leapTicksMax = 0;
        this.leapTargetIndex = -1;
        this.leapCooldownTicks = Math.max(0, ModConfig.leaping.leapCooldown);
        this.dweller.fallDistance = 0.0F;
        // Stop the residual horizontal motion; otherwise the dweller slides past the landing
        // block during the first tick after the controller releases it.
        this.dweller.motionX = 0.0D;
        this.dweller.motionZ = 0.0D;
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
        if (this.leaping) {
            return DwellerNodeType.WALK;
        }
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

    public boolean isLeaping() {
        return this.leaping;
    }

    public boolean canReachPosition(BlockPos pos) {
        Path path = this.getPathToPos(pos);
        if (path == null || path.getCurrentPathLength() == 0) {
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