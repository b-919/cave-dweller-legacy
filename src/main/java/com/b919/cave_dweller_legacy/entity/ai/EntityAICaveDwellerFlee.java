package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.util.TargetUtil;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EntityAICaveDwellerFlee extends EntityAIBase {
    private final CaveDwellerEntity entity;
    private final DwellerPursuit pursuit;
    private final double speedModifier;
    private int ticksUntilLeave;
    private boolean shouldLeave;
    private int retryCooldown;
    private double fleeX;
    private double fleeY;
    private double fleeZ;

    public EntityAICaveDwellerFlee(CaveDwellerEntity entity, double speedModifier) {
        this.entity = entity;
        this.pursuit = new DwellerPursuit(entity);
        this.speedModifier = speedModifier;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (entity.isInvisible()) {
            return false;
        }
        if (entity.currentRoll != Roll.FLEE) {
            return false;
        }
        return entity.getAttackTarget() != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.currentRoll != Roll.FLEE) {
            return false;
        }
        return entity.getAttackTarget() != null;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        shouldLeave = false;
        ticksUntilLeave = 600;
        retryCooldown = 0;
        this.pursuit.reset();
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target != null) {
            moveAway(target);
        }
    }

    @Override
    public void resetTask() {
        this.pursuit.reset();
    }

    @Override
    public void updateTask() {
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target == null) {
            entity.disappear();
            return;
        }
        ticksUntilLeave--;
        if (ticksUntilLeave <= 0) {
            shouldLeave = true;
        }
        if (shouldLeave && !TargetUtil.targetFacesMe(target, entity)) {
            entity.disappear();
            return;
        }
        if (entity.getNavigator().noPath()) {
            if (retryCooldown > 0) {
                retryCooldown--;
            } else if (!moveAway(target)) {
                retryCooldown = 20;
            }
        } else {
            this.pursuit.followPosition(this.fleeX, this.fleeY, this.fleeZ, speedModifier, 10);
        }
        entity.playFleeSound();
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
    }

    private boolean moveAway(EntityPlayer target) {
        Vec3d fallback = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3d fleePos = RandomPositionGenerator.findRandomTargetBlockAwayFrom(
                    entity, 32, 7, target.getPositionVector());
            if (fleePos == null) {
                continue;
            }
            if (fallback == null) {
                fallback = fleePos;
            }
            if (!DwellerPursuit.canReachPosition(entity, new BlockPos(fleePos))) {
                continue;
            }
            setFleeDestination(fleePos);
            return true;
        }
        if (fallback != null) {
            setFleeDestination(fallback);
            return true;
        }
        return false;
    }

    private void setFleeDestination(Vec3d fleePos) {
        this.fleeX = fleePos.x;
        this.fleeY = fleePos.y;
        this.fleeZ = fleePos.z;
        this.pursuit.reset();
        this.pursuit.followPosition(this.fleeX, this.fleeY, this.fleeZ, speedModifier, 10);
    }
}
