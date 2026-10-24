package com.b919.cave_dweller_legacy.util;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.common.ModNetwork;
import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.network.CaveSoundPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SpawnDirector {
    private static final Logger LOGGER = LogManager.getLogger(CDLMain.MODID);
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
            if (ModConfig.general.debugMode) {
                LOGGER.info("[DEBUG] Created spawn state for dimension {}", dim);
            }
        }

        if (ModConfig.general.fastForward) {
            if (state.spawnTimer > 20) state.spawnTimer = 20;
            if (state.noiseTimer > 20) state.noiseTimer = 20;
        }

        if (state.cooldownTimer > 0) {
            state.cooldownTimer--;
            if (state.cooldownTimer <= 0) {
                if (ModConfig.general.debugMode) {
                    LOGGER.info("[DEBUG] Cooldown ended for dimension {}", dim);
                }
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
            nearest = player;
            break;
        }

        if (nearest != null && state.aliveCount == 0) {
            if (state.noiseTimer <= 0) {
                if (ModConfig.general.debugMode) {
                    LOGGER.info("[DEBUG] Playing cave noise for dimension {}", dim);
                }
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
            float spawnChance = ModConfig.general.fastForward ? 1.0f : ModConfig.spawn.spawnChancePerTick;
            if (ModConfig.general.debugMode) {
                LOGGER.info("[DEBUG] Spawn timer expired, attempting spawn (chance {}/{}, alive={}, fastForward={})", spawnChance, ModConfig.spawn.spawnChancePerTick, state.aliveCount, ModConfig.general.fastForward);
            }
            if (rand.nextFloat() < spawnChance) {
                if (trySpawn(world, nearest)) {
                    if (ModConfig.general.debugMode) {
                        LOGGER.info("[DEBUG] Spawn succeeded in dimension {}", dim);
                    }
                    if (rand.nextDouble() < ModConfig.timers.cooldownChance) {
                        state.cooldownTimer = ModConfig.timers.cooldownAfterSpawn * 20;
                        state.hasSpawnedInCooldown = true;
                    } else {
                        state.reset();
                    }
                } else {
                    if (ModConfig.general.debugMode) {
                        LOGGER.info("[DEBUG] Spawn failed (no valid location)");
                    }
                    state.spawnTimer = 20;
                }
            } else {
                if (ModConfig.general.debugMode) {
                    LOGGER.info("[DEBUG] Spawn chance failed");
                }
                state.spawnTimer = 20;
            }
        }
    }

    private static void playCaveNoise(World world, EntityPlayer player) {
        if (ModConfig.sound.playAtPlayerPosition && ModNetwork.INSTANCE != null) {
            float volume = (float) ModConfig.sound.caveNoiseVolume;
            float pitch = (float) ModConfig.sound.caveNoisePitch;
            CaveSoundPacket packet = new CaveSoundPacket("cavenoise", player.posX, player.posY, player.posZ,
                    volume, pitch);
            ModNetwork.INSTANCE.sendToAllAround(packet, new NetworkRegistry.TargetPoint(player.dimension, player.posX, player.posY, player.posZ, 64));
            if (ModConfig.general.debugMode) {
                LOGGER.info("[DEBUG] Sent cave noise packet to player {} at ({},{},{}) vol={} pitch={}", player.getName(), player.posX, player.posY, player.posZ, volume, pitch);
            }
        }
    }

    private static boolean trySpawn(World world, EntityPlayer player) {
        Random rand = new Random();
        int attempts = 0;
        int failedNoValidY = 0;
        int maxAttempts = ModConfig.general.fastForward ? 200 : 40;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            attempts++;
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist = ModConfig.spawn.minSpawnDistanceFromPlayer + rand.nextDouble() * (ModConfig.spawn.spawnDistance - ModConfig.spawn.minSpawnDistanceFromPlayer);
            double x = player.posX + Math.cos(angle) * dist;
            double z = player.posZ + Math.sin(angle) * dist;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos((int) x, 0, (int) z);
            boolean foundValidY = false;
            int playerY = (int) player.posY;
            for (int y = playerY + 6; y > 0; y--) {
                pos.setY(y);
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
                if (!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) {
                    continue;
                }
                if (!world.isAirBlock(pos) || !world.isAirBlock(pos.up())) {
                    continue;
                }
                AxisAlignedBB box = new AxisAlignedBB(pos, pos.add(1, 2, 1));
                if (!world.checkNoEntityCollision(box)) {
                    continue;
                }
                foundValidY = true;
                break;
            }
            
            if (!foundValidY) {
                failedNoValidY++;
                continue;
            }
            
            CaveDwellerEntity entity = new CaveDwellerEntity(world);
            entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            entity.setAttackTarget(player);
            entity.setInvisible(true);
            boolean spawned = world.spawnEntity(entity);
            if (ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Spawn " + (spawned ? "SUCCESS" : "FAILED") + " at " + pos + " (invisible=" + entity.isInvisible() + ") after " + attempts + " attempts (noValidY=" + failedNoValidY + ")");
            }
            return spawned;
        }
        if (ModConfig.general.debugMode) {
            System.out.println("[CAVE_DWELLER_DEBUG] Spawn FAILED after " + attempts + " attempts (noValidY=" + failedNoValidY + ")");
        }
        return false;
    }
}
