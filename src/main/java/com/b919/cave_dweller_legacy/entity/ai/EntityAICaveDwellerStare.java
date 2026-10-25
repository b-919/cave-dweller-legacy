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
    private final DwellerPursuit pursuit;
    private boolean wasNotLookingPreviously = true;
    private int lookedAtCount;
    private int lookedAtMax;
    private final Random rand = new Random();
    private int stareTimeAccumulator;
    private int rollCooldown;

    public EntityAICaveDwellerStare(CaveDwellerEntity entity) {
        this.entity = entity;
        this.pursuit = new DwellerPursuit(entity);
        this.setMutexBits(3);
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        int min = Math.max(1, ModConfig.behavior.stareTimeMin);
        int max = Math.max(min, ModConfig.behavior.stareTimeMax);
        this.lookedAtMax = min + this.rand.nextInt(max - min + 1);
        this.pursuit.reset();
        this.rollCooldown = 0;
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
        rollCooldown = 0;
        this.pursuit.reset();
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
            if (stareTimeAccumulator >= Math.max(1, ModConfig.behavior.stareHoldTicks)) {
                lookedAtCount++;
                stareTimeAccumulator = 0;
                if (ModConfig.general.debugMode) {
                    System.out.println("[CAVE_DWELLER_DEBUG] Stare: Sustained look, count=" + lookedAtCount + "/" + lookedAtMax);
                }
            }
        }

        wasNotLookingPreviously = !actuallyLooking;

        if (lookedAtCount >= lookedAtMax) {
            if (rollCooldown > 0) {
                rollCooldown--;
            } else {
                rollCooldown = Math.max(1, ModConfig.behavior.chaseRollIntervalTicks);
                if (!actuallyLooking && rand.nextDouble() < 0.1) {
                    entity.disappear();
                    return;
                }
                if (rand.nextDouble() < 0.6) {
                    entity.pickRoll(Arrays.asList(Roll.CHASE, Roll.FLEE));
                } else {
                    entity.reRoll();
                }
                return;
            }
        }
        
        if (!actuallyLooking) {
            this.pursuit.followEntity(target, ModConfig.behavior.movementSpeed, 10);
        } else {
            entity.getNavigator().clearPath();
            this.pursuit.reset();
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
        }
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
    }
}