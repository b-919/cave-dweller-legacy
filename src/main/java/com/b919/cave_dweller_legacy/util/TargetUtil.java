package com.b919.cave_dweller_legacy.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

public class TargetUtil {
    public static boolean targetFacesMe(EntityPlayer player, net.minecraft.entity.EntityLivingBase entity) {
        if (player == null) {
            return false;
        }
        Vec3d look = player.getLookVec();
        Vec3d toMe = new Vec3d(entity.posX - player.posX,
                (entity.posY + entity.getEyeHeight()) - (player.posY + player.getEyeHeight()),
                entity.posZ - player.posZ);
        if (toMe.lengthVector() < 0.0001D) {
            return true;
        }
        toMe = toMe.normalize();
        double dot = look.dotProduct(toMe);
        return dot > 0.3D;
    }

    public static boolean playerCanSeeMe(EntityPlayer player, net.minecraft.entity.EntityLivingBase entity) {
        return player.canEntityBeSeen(entity);
    }
}
