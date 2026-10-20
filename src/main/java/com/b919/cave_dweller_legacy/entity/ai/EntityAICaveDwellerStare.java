package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.util.TargetUtil;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;


import java.util.Arrays;
import java.util.Random;

public class EntityAICaveDwellerStare extends EntityAIBase {
    private final CaveDwellerEntity entity;
    private boolean wasNotLookingPreviously;
    private int lookedAtCount;
    private final int lookedAtMax;
    private final Random rand = new Random();

    public EntityAICaveDwellerStare(CaveDwellerEntity entity) {
        this.entity = entity;
        this.setMutexBits(3);
        this.lookedAtMax = ModConfig.behavior.stareTimeMin + rand.nextInt(Math.max(1, ModConfig.behavior.stareTimeMax - ModConfig.behavior.stareTimeMin + 1));
    }

    @Override
    public boolean shouldExecute() {
        if (entity.isInvisible()) {
            return false;
        }
        if (entity.currentRoll != Roll.STARE) {
            return false;
        }
        if (entity.getAttackTarget() == null || !(entity.getAttackTarget() instanceof EntityPlayer)) {
            return false;
        }
        return TargetUtil.isValidTarget(entity.getAttackTarget(), entity);
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.getAttackTarget() == null) {
            return false;
        }
        return entity.currentRoll == Roll.STARE;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        lookedAtCount = 0;
        wasNotLookingPreviously = false;
        entity.pleaseStopMoving = false;
    }

    @Override
    public void updateTask() {
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target == null) {
            entity.disappear();
            return;
        }
        boolean actuallyLooking = TargetUtil.targetFacesMe(target, entity) && TargetUtil.playerCanSeeMe(target, entity);
        if (wasNotLookingPreviously && actuallyLooking) {
            lookedAtCount++;
        }
        if (lookedAtCount > lookedAtMax) {
            if (!actuallyLooking && rand.nextDouble() < 0.1) {
                entity.disappear();
            } else if (rand.nextDouble() < 0.3) {
                entity.pickRoll(Arrays.asList(Roll.CHASE, Roll.FLEE));
            } else {
                entity.reRoll();
            }
        }
        if (!actuallyLooking) {
            entity.pleaseStopMoving = false;
            entity.getNavigator().tryMoveToEntityLiving(target, 1.0D);
        } else {
            entity.pleaseStopMoving = true;
            entity.getNavigator().clearPath();
            entity.setVelocity(0, 0, 0);
        }
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        wasNotLookingPreviously = !actuallyLooking;
    }
}
