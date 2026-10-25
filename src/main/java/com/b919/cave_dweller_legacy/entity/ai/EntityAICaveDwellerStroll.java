package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class EntityAICaveDwellerStroll extends EntityAIBase {
    private final CaveDwellerEntity entity;
    private final DwellerPursuit pursuit;
    private double x;
    private double y;
    private double z;
    private int reRollTimer;
    private int searchCooldown;
    private int noPathTicks;

    public EntityAICaveDwellerStroll(CaveDwellerEntity entity) {
        this.entity = entity;
        this.pursuit = new DwellerPursuit(entity);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        if (entity.isInvisible()) {
            return false;
        }
        if (entity.currentRoll != Roll.STROLL) {
            if (entity.getAttackTarget() == null && entity.getRNG().nextInt(20) == 0) {
                entity.reRoll();
            }
            return false;
        }
        return findDestination();
    }

    private boolean findDestination() {
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3d candidate = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
            if (candidate == null || !DwellerPursuit.canReachPosition(this.entity, new BlockPos(candidate))) {
                continue;
            }
            this.x = candidate.x;
            this.y = candidate.y;
            this.z = candidate.z;
            return true;
        }
        this.searchCooldown = 20;
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return entity.currentRoll == Roll.STROLL && !entity.isInvisible();
    }

    @Override
    public void startExecuting() {
        this.pursuit.reset();
        this.pursuit.followPosition(this.x, this.y, this.z, ModConfig.behavior.strollSpeed, 10);
    }

    @Override
    public void resetTask() {
        this.pursuit.reset();
        this.reRollTimer = 0;
        this.noPathTicks = 0;
    }

    @Override
    public void updateTask() {
        if (this.entity.getNavigator().noPath()) {
            this.noPathTicks++;
        } else {
            this.noPathTicks = 0;
        }

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
        } else if (this.entity.getNavigator().noPath()
                && (this.entity.getDistanceSq(this.x, this.y, this.z) < 4.0D || this.noPathTicks > 40)) {
            if (findDestination()) {
                this.pursuit.reset();
                this.noPathTicks = 0;
            }
        }

        this.pursuit.followPosition(this.x, this.y, this.z, ModConfig.behavior.strollSpeed, 10);

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
