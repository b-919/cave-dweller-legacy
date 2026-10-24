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
    private boolean wasNotLookingPreviously = true;
    private int lookedAtCount;
    private final int lookedAtMax;
    private final Random rand = new Random();
    private int stareTimeAccumulator;

    public EntityAICaveDwellerStare(CaveDwellerEntity entity) {
        this.entity = entity;
        this.setMutexBits(3);
        this.lookedAtMax = ModConfig.behavior.stareTimeMin + rand.nextInt(Math.max(1, ModConfig.behavior.stareTimeMax - ModConfig.behavior.stareTimeMin + 1));
    }

    @Override
    public boolean shouldExecute() {
        if (entity.isInvisible()) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldExecute: false (invisible)");
            return false;
        }
        if (entity.currentRoll != Roll.STARE) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldExecute: false (roll=" + entity.currentRoll + ")");
            return false;
        }
        if (entity.getAttackTarget() == null || !(entity.getAttackTarget() instanceof EntityPlayer)) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldExecute: false (no target)");
            return false;
        }
        if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldExecute: true");
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.getAttackTarget() == null) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldContinueExecuting: false (no target)");
            return false;
        }
        boolean continueExec = entity.currentRoll == Roll.STARE;
        if (!continueExec) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Stare shouldContinueExecuting: false (roll=" + entity.currentRoll + ")");
        }
        return continueExec;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        lookedAtCount = 0;
        wasNotLookingPreviously = true;
        stareTimeAccumulator = 0;
    }

    @Override
    public void updateTask() {
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target == null) {
            entity.disappear();
            return;
        }
        
        if (entity.getRevengeTarget() == target) {
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Stare: Player attacked, instant CHASE");
            }
            entity.setRoll(Roll.CHASE);
            return;
        }
        
        boolean actuallyLooking = TargetUtil.targetFacesMe(target, entity) && TargetUtil.playerCanSeeMe(target, entity);
        if (wasNotLookingPreviously && actuallyLooking) {
            lookedAtCount++;
            stareTimeAccumulator = 0;
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Stare: Look detected, count=" + lookedAtCount + "/" + lookedAtMax);
            }
        } else if (actuallyLooking) {
            stareTimeAccumulator++;
            if (stareTimeAccumulator > 100) {
                lookedAtCount++;
                stareTimeAccumulator = 0;
                if (ModConfig.general.debugMode) {
                    System.out.println("[CAVE_DWELLER_DEBUG] Stare: Sustained look, count=" + lookedAtCount + "/" + lookedAtMax);
                }
            }
        }
        
        if (lookedAtCount >= lookedAtMax) {
            if (!actuallyLooking && rand.nextDouble() < 0.1) {
                entity.disappear();
            } else if (rand.nextDouble() < 0.6) {
                entity.pickRoll(Arrays.asList(Roll.CHASE, Roll.FLEE));
            } else {
                entity.reRoll();
            }
            return;
        }
        
        if (!actuallyLooking) {
            entity.getNavigator().tryMoveToEntityLiving(target, ModConfig.behavior.movementSpeed);
        } else {
            entity.getNavigator().clearPath();
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
        }
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        wasNotLookingPreviously = !actuallyLooking;
    }
}