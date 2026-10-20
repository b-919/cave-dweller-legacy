package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.util.TargetUtil;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

public class EntityAICaveDwellerFlee extends EntityAIBase {
    private final CaveDwellerEntity entity;
    private final double speedModifier;
    private int ticksUntilLeave;
    private boolean shouldLeave;

    public EntityAICaveDwellerFlee(CaveDwellerEntity entity, double speedModifier) {
        this.entity = entity;
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
        ticksUntilLeave = 300;
        assert entity.getAttackTarget() != null;
        Vec3d fleePos = RandomPositionGenerator.findRandomTargetBlockAwayFrom(entity, 32, 7, entity.getAttackTarget().getPositionVector());
        if (fleePos != null) {
            entity.getNavigator().tryMoveToXYZ(fleePos.x, fleePos.y, fleePos.z, speedModifier);
        }
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
            Vec3d fleePos = RandomPositionGenerator.findRandomTargetBlockAwayFrom(entity, 32, 7, target.getPositionVector());
            if (fleePos != null) {
                entity.getNavigator().tryMoveToXYZ(fleePos.x, fleePos.y, fleePos.z, speedModifier);
            }
        }
        entity.playFleeSound();
        entity.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
    }
}
