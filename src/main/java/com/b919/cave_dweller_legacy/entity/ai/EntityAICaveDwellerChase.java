package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

public class EntityAICaveDwellerChase extends EntityAIBase {
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private final CaveDwellerEntity entity;
    private int attackCooldown = 0;
    private int pathRecalc = 0;

    public EntityAICaveDwellerChase(CaveDwellerEntity entity) {
        this.entity = entity;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (entity.isInvisible()) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldExecute: false (invisible)");
            return false;
        }
        if (entity.currentRoll != Roll.CHASE) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldExecute: false (roll=" + entity.currentRoll + ")");
            return false;
        }
        if (entity.getAttackTarget() == null || !(entity.getAttackTarget() instanceof EntityPlayer)) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldExecute: false (no target)");
            return false;
        }
        if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldExecute: true");
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.currentRoll != Roll.CHASE) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldContinueExecuting: false (roll=" + entity.currentRoll + ")");
            return false;
        }
        boolean hasTarget = entity.getAttackTarget() != null;
        if (!hasTarget) {
            if (ModConfig.general.debugMode) System.out.println("[CAVE_DWELLER_DEBUG] Chase shouldContinueExecuting: false (no target)");
        }
        return hasTarget;
    }

    @Override
    public void resetTask() {
        super.resetTask();
    }

    @Override
    public void updateTask() {
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target == null) {
            return;
        }
        
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (pathRecalc++ % 5 == 0) {
            double speed = ModConfig.behavior.chaseSpeed;
            entity.getNavigator().tryMoveToEntityLiving(target, speed);
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Chase: path recalc, speed=" + speed + " hitbox=" + entity.width + "x" + entity.height);
            }
        }
        double dist = entity.getDistanceSq(target);
        double reach = ModConfig.behavior.attackReach;
        if (ModConfig.general.debugMode) {
            double attackDmg = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            System.out.println("[CAVE_DWELLER_DEBUG] Chase: dist=" + Math.sqrt(dist) + " reach=" + reach + " attackCooldown=" + attackCooldown + " attackDamage=" + attackDmg);
        }
        if (dist <= reach * reach) {
            if (attackCooldown <= 0) {
                if (!entity.canEntityBeSeen(target)) {
                    if (ModConfig.general.debugMode) {
                        System.out.println("[CAVE_DWELLER_DEBUG] Chase: Target not visible (blocked by blocks)");
                    }
                } else {
                    if (ModConfig.general.debugMode) {
                        System.out.println("[CAVE_DWELLER_DEBUG] Chase: ATTACKING target at distance " + Math.sqrt(dist) + " attackDamage=" + entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
                    }
                    boolean attacked = entity.attackEntityAsMob(target);
                    entity.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                    if (ModConfig.general.debugMode) {
                        System.out.println("[CAVE_DWELLER_DEBUG] Chase: attackEntityAsMob returned " + attacked);
                    }
                    attackCooldown = ATTACK_COOLDOWN_TICKS;
                }
            }
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }

}
