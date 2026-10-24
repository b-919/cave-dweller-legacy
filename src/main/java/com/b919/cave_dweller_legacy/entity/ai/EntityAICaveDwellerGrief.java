package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.entity.ai.path.PathNavigateDweller;
import com.b919.cave_dweller_legacy.util.GriefUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class EntityAICaveDwellerGrief extends EntityAIBase {
    private static final int PATH_CHECK_INTERVAL = 10;
    private static final double REACHABLE_PATH_DISTANCE_SQ = 4.0D;

    private final CaveDwellerEntity entity;
    private EntityPlayer target;
    private UUID trackedTarget;
    private int lastPathCheckTick = -1;
    private int unreachableTicks;
    private int lastMoveTick;
    private BlockPos breakingPos;
    private int breakingTicks;
    private int breakDuration;
    private boolean tunnelMode;

    public EntityAICaveDwellerGrief(CaveDwellerEntity entity) {
        this.entity = entity;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!ModConfig.grief.enabled || this.entity.world.isRemote || this.entity.isDead) {
            return false;
        }

        EntityPlayer player = getValidTarget();
        if (player == null) {
            resetUnreachableTracking();
            return false;
        }

        if (this.entity.currentRoll == Roll.CHASE) {
            this.tunnelMode = false;
            if (!isUnreachableLongEnough(player)) {
                return false;
            }
            return findDigTarget(player) != null;
        }

        resetUnreachableTracking();
        this.tunnelMode = ModConfig.grief.tunnelCarverMode;
        return this.tunnelMode && this.entity.getDistance(player) <= ModConfig.grief.tunnelRange
                && this.entity.getDistance(player) > ModConfig.grief.tunnelStopDistance;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (!ModConfig.grief.enabled || this.entity.world.isRemote || this.entity.isDead) {
            return false;
        }
        EntityPlayer player = getValidTarget();
        if (player == null || this.target == null || !player.getUniqueID().equals(this.target.getUniqueID())) {
            return false;
        }
        if (this.tunnelMode) {
            return ModConfig.grief.tunnelCarverMode && this.entity.currentRoll != Roll.CHASE
                    && this.entity.getDistance(player) <= ModConfig.grief.tunnelRange
                    && this.entity.getDistance(player) > ModConfig.grief.tunnelStopDistance;
        }
        return this.entity.currentRoll == Roll.CHASE && isUnreachableLongEnough(player)
                && findDigTarget(player) != null;
    }

    @Override
    public void startExecuting() {
        this.target = getValidTarget();
        if (this.target != null && !this.target.getUniqueID().equals(this.trackedTarget)) {
            resetUnreachableTracking();
            this.trackedTarget = this.target.getUniqueID();
        }
        this.breakingPos = null;
        this.breakingTicks = 0;
        this.breakDuration = 0;
        this.lastMoveTick = -PATH_CHECK_INTERVAL;
    }

    @Override
    public void resetTask() {
        clearBreakProgress();
        if (this.tunnelMode) {
            this.entity.getNavigator().clearPath();
        }
        this.target = null;
        this.tunnelMode = false;
    }

    @Override
    public void updateTask() {
        EntityPlayer player = getValidTarget();
        if (player == null) {
            return;
        }

        this.entity.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);
        BlockPos digTarget = findDigTarget(player);
        if (digTarget != null) {
            this.entity.getNavigator().clearPath();
            breakBlock(digTarget);
        } else {
            clearBreakProgress();
            if (this.tunnelMode && this.entity.ticksExisted - this.lastMoveTick >= PATH_CHECK_INTERVAL) {
                this.lastMoveTick = this.entity.ticksExisted;
                this.entity.getNavigator().tryMoveToEntityLiving(player, ModConfig.behavior.strollSpeed);
            }
        }
    }

    private EntityPlayer getValidTarget() {
        if (!(this.entity.getAttackTarget() instanceof EntityPlayer)) {
            return null;
        }
        EntityPlayer player = (EntityPlayer) this.entity.getAttackTarget();
        return player.isEntityAlive() && !player.isCreative() && !player.isSpectator() ? player : null;
    }

    private boolean isUnreachableLongEnough(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        if (!playerId.equals(this.trackedTarget)) {
            resetUnreachableTracking();
            this.trackedTarget = playerId;
            this.lastPathCheckTick = this.entity.ticksExisted;
            boolean unreachable = !hasCompletePath(player);
            this.unreachableTicks = unreachable && ModConfig.grief.unreachableDelayTicks <= 0 ? 1 : 0;
            return ModConfig.grief.unreachableDelayTicks <= 0 && unreachable;
        }

        int now = this.entity.ticksExisted;
        if (this.lastPathCheckTick < 0) {
            this.lastPathCheckTick = now;
        }
        int elapsed = now - this.lastPathCheckTick;
        if (elapsed >= PATH_CHECK_INTERVAL) {
            this.lastPathCheckTick = now;
            if (hasCompletePath(player)) {
                this.unreachableTicks = 0;
            } else {
                this.unreachableTicks += elapsed;
            }
        }
        return ModConfig.grief.unreachableDelayTicks <= 0
                ? this.unreachableTicks > 0
                : this.unreachableTicks >= ModConfig.grief.unreachableDelayTicks;
    }

    private boolean hasCompletePath(EntityPlayer player) {
        Path path = this.entity.getNavigator().getPathToEntityLiving(player);
        if (path == null || path.getCurrentPathLength() == 0) {
            return false;
        }
        PathPoint end = path.getPathPointFromIndex(path.getCurrentPathLength() - 1);
        double dx = end.x + 0.5D - player.posX;
        double dy = end.y - player.getEntityBoundingBox().minY;
        double dz = end.z + 0.5D - player.posZ;
        return dx * dx + dy * dy + dz * dz <= REACHABLE_PATH_DISTANCE_SQ;
    }

    private BlockPos findDigTarget(EntityPlayer player) {
        double dx = player.posX - this.entity.posX;
        double dz = player.posZ - this.entity.posZ;
        EnumFacing direction;
        if (Math.abs(dx) >= Math.abs(dz)) {
            direction = dx >= 0.0D ? EnumFacing.EAST : EnumFacing.WEST;
        } else {
            direction = dz >= 0.0D ? EnumFacing.SOUTH : EnumFacing.NORTH;
        }

        BlockPos feet = new BlockPos(this.entity.posX, this.entity.getEntityBoundingBox().minY, this.entity.posZ);
        BlockPos front = feet.offset(direction);
        BlockPos[] candidates;
        if (player.posY > this.entity.posY + 1.0D) {
            candidates = new BlockPos[]{feet.up(2), front.up(2), front.up(), front};
        } else if (player.posY < this.entity.posY - 1.0D) {
            candidates = new BlockPos[]{front.down(), front, feet.down()};
        } else {
            candidates = new BlockPos[]{front, front.up(), front.up(2)};
        }

        for (BlockPos candidate : candidates) {
            IBlockState state = this.entity.world.getBlockState(candidate);
            if (GriefUtil.canBreak(this.entity.world, candidate, state)) {
                return candidate;
            }
        }
        return null;
    }

    private void breakBlock(BlockPos pos) {
        IBlockState state = this.entity.world.getBlockState(pos);
        if (!GriefUtil.canBreak(this.entity.world, pos, state)) {
            clearBreakProgress();
            return;
        }

        if (!pos.equals(this.breakingPos)) {
            clearBreakProgress();
            this.breakingPos = pos.toImmutable();
            float hardness = state.getBlockHardness(this.entity.world, pos);
            double speed = Math.max(0.1D, ModConfig.grief.blockBreakSpeed);
            this.breakDuration = (int) Math.min(Integer.MAX_VALUE,
                    Math.max(10.0D, Math.ceil(hardness * 20.0D / speed)));
        }

        this.breakingTicks++;
        int stage = (int) Math.min(9L, (long) this.breakingTicks * 10L / Math.max(1, this.breakDuration));
        this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), pos, stage);
        if (this.breakingTicks >= this.breakDuration) {
            this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), pos, -1);
            this.entity.world.playEvent(2001, pos, net.minecraft.block.Block.getStateId(state));
            this.entity.world.destroyBlock(pos, ModConfig.grief.dropBrokenBlocks);
            if (this.entity.getNavigator() instanceof PathNavigateDweller) {
                ((PathNavigateDweller) this.entity.getNavigator()).onBlockChanged(pos);
            }
            this.breakingPos = null;
            this.breakingTicks = 0;
            this.breakDuration = 0;
        }
    }

    private void clearBreakProgress() {
        if (this.breakingPos != null && !this.entity.world.isRemote) {
            this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), this.breakingPos, -1);
        }
        this.breakingPos = null;
        this.breakingTicks = 0;
        this.breakDuration = 0;
    }

    private void resetUnreachableTracking() {
        this.trackedTarget = null;
        this.lastPathCheckTick = -1;
        this.unreachableTicks = 0;
    }
}
