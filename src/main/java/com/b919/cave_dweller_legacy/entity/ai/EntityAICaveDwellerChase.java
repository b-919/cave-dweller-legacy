package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.util.TargetUtil;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

public class EntityAICaveDwellerChase extends EntityAIBase {
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
            return false;
        }
        if (entity.currentRoll != Roll.CHASE) {
            return false;
        }
        if (entity.getAttackTarget() == null || !(entity.getAttackTarget() instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity.getAttackTarget();
        return TargetUtil.targetFacesMe(player, entity) && entity.getEntitySenses().canSee(player);
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.currentRoll != Roll.CHASE) {
            return false;
        }
        return entity.getAttackTarget() != null;
    }

    @Override
    public void resetTask() {
        super.resetTask();
        entity.pleaseStopMoving = false;
    }

    @Override
    public void updateTask() {
        EntityPlayer target = (EntityPlayer) entity.getAttackTarget();
        if (target == null) {
            return;
        }
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (pathRecalc++ % 10 == 0) {
            entity.getNavigator().tryMoveToEntityLiving(target, ModConfig.behavior.chaseSpeed);
        }
        double dist = entity.getDistanceSq(target);
        if (dist <= (entity.width * 4.0F) * (entity.width * 4.0F) + target.width) {
            if (attackCooldown <= 0) {
                entity.attackEntityAsMob(target);
                entity.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                attackCooldown = 20;
            }
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
}
