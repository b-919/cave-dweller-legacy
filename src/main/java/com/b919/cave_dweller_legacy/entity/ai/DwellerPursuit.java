package com.b919.cave_dweller_legacy.entity.ai;

import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.ai.path.PathNavigateDweller;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class DwellerPursuit {
    private static final double STUCK_DISTANCE_SQ = 0.09D;

    private final CaveDwellerEntity entity;
    private int repathCooldown;
    private int stuckCheckCooldown;
    private Vec3d lastStuckCheckPos;

    public DwellerPursuit(CaveDwellerEntity entity) {
        this.entity = entity;
        this.lastStuckCheckPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
    }

    public boolean followEntity(Entity target, double speed, int repathInterval) {
        if (target == null) {
            return false;
        }
        return follow(target, null, speed, repathInterval);
    }

    public boolean followPosition(double x, double y, double z, double speed, int repathInterval) {
        return follow(null, new Vec3d(x, y, z), speed, repathInterval);
    }

    private boolean follow(Entity target, Vec3d position, double speed, int repathInterval) {
        if (this.entity.isLeaping()) {
            this.lastStuckCheckPos = new Vec3d(this.entity.posX, this.entity.posY, this.entity.posZ);
            return !this.entity.getNavigator().noPath();
        }

        int interval = Math.max(1, repathInterval);
        boolean repathNow = false;

        if (this.repathCooldown-- <= 0) {
            this.repathCooldown = interval;
            repathNow = true;
        }

        if (this.stuckCheckCooldown-- <= 0) {
            this.stuckCheckCooldown = interval;
            Vec3d currentPos = new Vec3d(this.entity.posX, this.entity.posY, this.entity.posZ);
            if (!this.entity.getNavigator().noPath()
                    && currentPos.squareDistanceTo(this.lastStuckCheckPos) < STUCK_DISTANCE_SQ) {
                this.entity.getNavigator().clearPath();
                this.repathCooldown = interval;
                repathNow = true;
            }
            this.lastStuckCheckPos = currentPos;
        }

        if (repathNow) {
            if (target != null) {
                this.entity.getNavigator().tryMoveToEntityLiving(target, speed);
            } else {
                this.entity.getNavigator().tryMoveToXYZ(position.x, position.y, position.z, speed);
            }
        }
        return !this.entity.getNavigator().noPath();
    }

    public void reset() {
        this.repathCooldown = 0;
        this.stuckCheckCooldown = 0;
        this.lastStuckCheckPos = new Vec3d(this.entity.posX, this.entity.posY, this.entity.posZ);
    }

    public static boolean canReachPosition(CaveDwellerEntity entity, BlockPos pos) {
        PathNavigate navigator = entity.getNavigator();
        if (!(navigator instanceof PathNavigateDweller)) {
            return true;
        }
        return ((PathNavigateDweller) navigator).canReachPosition(pos);
    }
}
