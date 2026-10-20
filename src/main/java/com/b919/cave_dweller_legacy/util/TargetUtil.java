package com.b919.cave_dweller_legacy.util;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class TargetUtil {
    public static boolean isValidTarget(EntityLivingBase target, net.minecraft.entity.EntityCreature entity) {
        if (target == null || !target.isEntityAlive()) {
            return false;
        }
        if (!(target instanceof EntityPlayer)) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) target;
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (target.isInvisible()) {
            return false;
        }
        double dist = entity.getDistance(target);
        if (dist > ModConfig.behavior.followRange || dist > ModConfig.behavior.spottingRange) {
            return false;
        }
        return entity.canEntityBeSeen(target);
    }

    public static EntityPlayer getNearestValidPlayer(World world, net.minecraft.entity.EntityCreature entity) {
        EntityPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;

        List<EntityPlayer> players = new ArrayList<EntityPlayer>(world.playerEntities);
        for (EntityPlayer player : players) {
            if (player == null || !player.isEntityAlive()) {
                continue;
            }
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            BlockPos pos = player.getPosition();
            if (pos.getY() >= ModConfig.spawn.spawnHeightLimit && !ModConfig.spawn.allowSurfaceSpawn) {
                continue;
            }
            int skyLight = world.getLightFor(EnumSkyBlock.SKY, pos);
            int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
            if (skyLight > ModConfig.spawn.skyLightLevel || blockLight > ModConfig.spawn.blockLightLevel) {
                continue;
            }
            boolean dimOk = false;
            int currentDim = world.provider.getDimension();
            for (int d : ModConfig.spawn.dimensionWhitelist) {
                if (d == currentDim) {
                    dimOk = true;
                    break;
                }
            }
            if (!dimOk) {
                continue;
            }
            double dist = entity.getDistance(player);
            if (dist <= ModConfig.spawn.spawnDistance && dist < nearestDist) {
                if (!ModConfig.spawn.checkPathToSpawn || entity.getNavigator().getPathToEntityLiving(player) != null) {
                    nearest = player;
                    nearestDist = dist;
                } else if (dist < ModConfig.spawn.minSpawnDistanceFromPlayer) {
                    nearest = player;
                    nearestDist = dist;
                }
            }
        }
        return nearest;
    }

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
        return dot > 0.99D;
    }

    public static boolean playerCanSeeMe(EntityPlayer player, net.minecraft.entity.EntityLivingBase entity) {
        return player.canEntityBeSeen(entity);
    }
}
