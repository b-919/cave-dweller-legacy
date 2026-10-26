package com.b919.cave_dweller_legacy.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

/** Shared trajectory math for gap leaping.*/
public final class DwellerLeap {
    private static final double BODY_HALF_WIDTH = 0.3D;
    private static final double BODY_HEIGHT = 2.7D;
    private static final double GRAVITY = 0.08D;
    private static final double DRAG = 0.98D;
    private static final double GRAVITY_TERM = GRAVITY * DRAG / (1.0D - DRAG);
    private static final double LEAP_CLEARANCE = 0.15D;
    private static final double MAX_SAMPLE_STEP = 0.5D;
    private static final int MAX_SAMPLES_PER_TICK = 32;

    private DwellerLeap() {
    }

    public static int flightTicks(double distance, double speed, int maxTicks) {
        int ticks = MathHelper.ceil(distance / Math.max(0.05D, speed));
        return MathHelper.clamp(ticks, 1, Math.max(1, maxTicks));
    }

    public static double horizontalSpeed(double distance, int ticks) {
        if (ticks <= 0) {
            return 0.0D;
        }
        return Math.max(0.05D, distance / ticks);
    }
    public static double launchMotionY(double rise, int ticks) {
        if (ticks <= 0) {
            return 0.0D;
        }
        double targetRise = rise + LEAP_CLEARANCE;
        double decay = Math.pow(DRAG, ticks);
        double denominator = 1.0D - decay;
        if (denominator <= 1.0E-9D) {
            return 0.0D;
        }
        return (targetRise + GRAVITY_TERM * ticks) * (1.0D - DRAG) / denominator - GRAVITY_TERM;
    }

    public static double displacement(double launchMotionY, int ticks) {
        if (ticks <= 0) {
            return 0.0D;
        }
        return (launchMotionY + GRAVITY_TERM) * (1.0D - Math.pow(DRAG, ticks)) / (1.0D - DRAG)
                - GRAVITY_TERM * ticks;
    }

    public static boolean canTraverse(IBlockAccess world, int startX, int startY, int startZ,
                                      int targetX, int targetY, int targetZ,
                                      double speed, int maxTicks, int maxRise, int maxDrop) {
        int deltaX = targetX - startX;
        int deltaZ = targetZ - startZ;
        if (deltaX == 0 && deltaZ == 0) {
            return false;
        }

        int rise = targetY - startY;
        if (rise > maxRise || rise < -maxDrop) {
            return false;
        }

        double distance = Math.sqrt((double) deltaX * deltaX + (double) deltaZ * deltaZ);
        if (distance < 1.0D) {
            return false;
        }

        int ticks = flightTicks(distance, speed, maxTicks);
        double launch = launchMotionY(rise, ticks);
        double startCenterX = startX + 0.5D;
        double startCenterZ = startZ + 0.5D;
        boolean supportedStart = isSupported(world, startX, startY, startZ);
        int samplesPerTick = MathHelper.clamp(
                MathHelper.ceil(distance / (double) ticks / MAX_SAMPLE_STEP),
                1, MAX_SAMPLES_PER_TICK);
        double firstTickClamp = supportedStart ? Math.min(0.0D, launch) : 0.0D;
        double previousFeetY = startY;

        for (int tick = 1; tick <= ticks; tick++) {
            double feetY = startY + displacement(launch, tick) - firstTickClamp;

            double fromX = startCenterX + deltaX * (double) (tick - 1) / (double) ticks;
            double fromZ = startCenterZ + deltaZ * (double) (tick - 1) / (double) ticks;
            double toX = startCenterX + deltaX * (double) tick / (double) ticks;
            double toZ = startCenterZ + deltaZ * (double) tick / (double) ticks;

            for (int sample = 1; sample <= samplesPerTick; sample++) {
                double progress = (double) sample / (double) samplesPerTick;
                double x = fromX + (toX - fromX) * progress;
                double z = fromZ + (toZ - fromZ) * progress;
                double sampleFeetY = previousFeetY + (feetY - previousFeetY) * progress;
                if (!isBodyClear(world, x, sampleFeetY, z)) {
                    return false;
                }
            }
            previousFeetY = feetY;
        }
        return true;
    }

    private static boolean isBodyClear(IBlockAccess world, double centerX, double feetY, double centerZ) {
        AxisAlignedBB body = new AxisAlignedBB(
                centerX - BODY_HALF_WIDTH, feetY + 0.001D, centerZ - BODY_HALF_WIDTH,
                centerX + BODY_HALF_WIDTH, feetY + BODY_HEIGHT, centerZ + BODY_HALF_WIDTH);
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
                    AxisAlignedBB collisionBox = state.getCollisionBoundingBox(world, pos);
                    if (collisionBox != null && collisionBox != Block.NULL_AABB
                            && body.intersects(collisionBox.offset(pos))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isSupported(IBlockAccess world, int x, int y, int z) {
        BlockPos supportPos = new BlockPos(x, y - 1, z);
        IBlockState state = world.getBlockState(supportPos);
        return state.getBlock().isSideSolid(state, world, supportPos, EnumFacing.UP);
    }
}
