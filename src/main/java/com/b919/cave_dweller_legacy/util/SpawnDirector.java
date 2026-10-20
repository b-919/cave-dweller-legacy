package com.b919.cave_dweller_legacy.util;

import com.b919.cave_dweller_legacy.common.ModNetwork;
import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.network.CaveSoundPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpawnDirector {
    private static final Map<Integer, DimensionState> states = new HashMap<Integer, DimensionState>();

    public static class DimensionState {
        public int spawnTimer;
        public int noiseTimer;
        public int cooldownTimer;
        public int aliveCount;
        public boolean hasSpawnedInCooldown;

        public DimensionState() {
            reset();
        }

        public void reset() {
            Random rand = new Random();
            spawnTimer = (ModConfig.timers.timeUntilSpawnMin + rand.nextInt(Math.max(1, ModConfig.timers.timeUntilSpawnMax - ModConfig.timers.timeUntilSpawnMin + 1))) * 20;
            noiseTimer = (ModConfig.timers.resetNoiseMin + rand.nextInt(Math.max(1, ModConfig.timers.resetNoiseMax - ModConfig.timers.resetNoiseMin + 1))) * 20;
            cooldownTimer = 0;
            hasSpawnedInCooldown = false;
        }
    }

    public static void tick(World world) {
        if (world.isRemote) {
            return;
        }
        int dim = world.provider.getDimension();
        DimensionState state = states.get(dim);
        if (state == null) {
            state = new DimensionState();
            states.put(dim, state);
        }

        if (ModConfig.general.fastForward) {
            if (state.spawnTimer > 20) state.spawnTimer = 20;
            if (state.noiseTimer > 20) state.noiseTimer = 20;
        }

        if (state.cooldownTimer > 0) {
            state.cooldownTimer--;
            if (state.cooldownTimer <= 0) {
                state.reset();
            }
            return;
        }

        state.aliveCount = 0;
        for (Entity entity : world.loadedEntityList) {
            if (entity instanceof CaveDwellerEntity) {
                if (entity.isEntityAlive()) {
                    state.aliveCount++;
                }
            }
        }

        if (state.noiseTimer > 0) {
            state.noiseTimer--;
        }

        EntityPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (EntityPlayer player : world.playerEntities) {
            if (player == null || !player.isEntityAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            boolean dimOk = false;
            for (int d : ModConfig.spawn.dimensionWhitelist) {
                if (d == dim) {
                    dimOk = true;
                    break;
                }
            }
            if (!dimOk) {
                continue;
            }
            double dist = 0;
            CaveDwellerEntity dummy = new CaveDwellerEntity(world);
            dist = dummy.getDistance(player);
            dummy.setDead();
            if (dist < nearestDist && dist <= ModConfig.spawn.spawnDistance) {
                nearest = player;
                nearestDist = dist;
            }
        }

        if (nearest != null && state.aliveCount == 0) {
            if (state.noiseTimer <= 0) {
                playCaveNoise(world, nearest);
                Random rand = new Random();
                state.noiseTimer = (ModConfig.timers.resetNoiseMin + rand.nextInt(Math.max(1, ModConfig.timers.resetNoiseMax - ModConfig.timers.resetNoiseMin + 1))) * 20;
            }
        }

        if (state.spawnTimer > 0) {
            state.spawnTimer--;
        }

        if (state.spawnTimer <= 0 && state.aliveCount < ModConfig.spawn.maxSpawned && nearest != null) {
            Random rand = new Random();
            if (rand.nextFloat() < ModConfig.spawn.spawnChancePerTick) {
                if (trySpawn(world, nearest)) {
                    if (rand.nextDouble() < ModConfig.timers.cooldownChance) {
                        state.cooldownTimer = ModConfig.timers.cooldownAfterSpawn * 20;
                        state.hasSpawnedInCooldown = true;
                    } else {
                        state.reset();
                    }
                } else {
                    state.spawnTimer = 20;
                }
            } else {
                state.spawnTimer = 20;
            }
        }
    }

    private static void playCaveNoise(World world, EntityPlayer player) {
        if (ModConfig.sound.playAtPlayerPosition && ModNetwork.INSTANCE != null) {
            CaveSoundPacket packet = new CaveSoundPacket("cavenoise", player.posX, player.posY, player.posZ,
                    (float) ModConfig.sound.caveNoiseVolume, (float) ModConfig.sound.caveNoisePitch);
            ModNetwork.INSTANCE.sendToAllAround(packet, new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64));
        }
    }

    private static boolean trySpawn(World world, EntityPlayer player) {
        Random rand = new Random();
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist = ModConfig.spawn.minSpawnDistanceFromPlayer + rand.nextDouble() * (ModConfig.spawn.spawnDistance - ModConfig.spawn.minSpawnDistanceFromPlayer);
            double x = player.posX + Math.cos(angle) * dist;
            double z = player.posZ + Math.sin(angle) * dist;
            double y = player.posY + (rand.nextInt(13) - 6);
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            if (pos.getY() >= ModConfig.spawn.spawnHeightLimit && !ModConfig.spawn.allowSurfaceSpawn) {
                continue;
            }
            if (!world.canSeeSky(pos) && !ModConfig.spawn.allowSurfaceSpawn) {
                int skyLight = world.getLightFor(EnumSkyBlock.SKY, pos);
                if (skyLight > ModConfig.spawn.skyLightLevel) {
                    continue;
                }
            }
            if (!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) {
                continue;
            }
            if (!world.isAirBlock(pos) || !world.isAirBlock(pos.up())) {
                continue;
            }
            AxisAlignedBB box = new AxisAlignedBB(pos, pos.add(1, 2, 1));
            if (!world.checkNoEntityCollision(box)) {
                continue;
            }
            CaveDwellerEntity entity = new CaveDwellerEntity(world);
            entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            entity.setAttackTarget(player);
            entity.setInvisible(true);
            world.spawnEntity(entity);
            return true;
        }
        return false;
    }
}
