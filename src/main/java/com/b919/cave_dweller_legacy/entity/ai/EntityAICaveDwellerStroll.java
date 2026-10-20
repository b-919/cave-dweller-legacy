package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

public class EntityAICaveDwellerStroll extends EntityAIBase {
    private final CaveDwellerEntity entity;
    private double x;
    private double y;
    private double z;
    private int reRollTimer;

    public EntityAICaveDwellerStroll(CaveDwellerEntity entity) {
        this.entity = entity;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (entity.isInvisible()) {
            return false;
        }
        if (entity.currentRoll != Roll.STROLL && entity.getAttackTarget() == null) {
            entity.reRoll();
        }
        if (entity.currentRoll != Roll.STROLL) {
            return false;
        }
        if (entity.pleaseStopMoving) {
            return false;
        }
        Vec3d vec3d = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
        if (vec3d == null) {
            return false;
        }
        this.x = vec3d.x;
        this.y = vec3d.y;
        this.z = vec3d.z;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (entity.currentRoll != Roll.STROLL) {
            return false;
        }
        return !this.entity.getNavigator().noPath();
    }

    @Override
    public void startExecuting() {
        this.entity.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, 0.3D);
    }

    @Override
    public void updateTask() {
        reRollTimer++;
        if (reRollTimer > 200) {
            if (entity.getAttackTarget() != null && entity.getRNG().nextInt(3) != 0) {
                entity.reRoll();
            } else if (entity.getAttackTarget() == null && entity.getRNG().nextInt(5) == 0) {
                entity.reRoll();
            }
            reRollTimer = 0;
        }
    }
}
