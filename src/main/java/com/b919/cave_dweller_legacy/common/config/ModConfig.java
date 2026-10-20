package com.b919.cave_dweller_legacy.common.config;

import com.b919.cave_dweller_legacy.CDLMain;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = CDLMain.MODID)
@Config.LangKey("config.cave_dweller_legacy.title")
public class ModConfig {
    @Config.Name("Spawn")
    public static Spawn spawn = new Spawn();

    @Config.Name("Timers")
    public static Timers timers = new Timers();

    @Config.Name("Behavior")
    public static Behavior behavior = new Behavior();

    @Config.Name("Sound")
    public static Sound sound = new Sound();

    @Config.Name("General")
    public static General general = new General();

    public static class Spawn {
        @Config.Name("Spawn Chance Per Tick")
        @Config.Comment("Chance per tick to attempt spawning when timer expires (0.0 - 1.0)")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public float spawnChancePerTick = 0.005F;

        @Config.Name("Spawn Height Limit")
        @Config.Comment("Maximum Y level to spawn below")
        @Config.RangeInt(min = 0, max = 256)
        public int spawnHeightLimit = 40;

        @Config.Name("Allow Surface Spawn")
        @Config.Comment("Allow spawning on the surface (not just underground)")
        public boolean allowSurfaceSpawn = false;

        @Config.Name("Max Spawned")
        @Config.Comment("Maximum number of Cave Dwellers alive at once")
        @Config.RangeInt(min = 1, max = 10)
        public int maxSpawned = 1;

        @Config.Name("Spawn Distance From Player")
        @Config.Comment("Maximum distance to spawn from player")
        @Config.RangeInt(min = 16, max = 128)
        public int spawnDistance = 64;

        @Config.Name("Min Spawn Distance From Player")
        @Config.Comment("Minimum distance to spawn from player")
        @Config.RangeInt(min = 4, max = 32)
        public int minSpawnDistanceFromPlayer = 16;

        @Config.Name("Check Path To Spawn")
        @Config.Comment("Check if there is a path from spawn location to player")
        public boolean checkPathToSpawn = true;

        @Config.Name("Dimension Whitelist")
        @Config.Comment("Dimensions where Cave Dweller can spawn (e.g., 0 for Overworld)")
        public int[] dimensionWhitelist = new int[]{0};

        @Config.Name("Sky Light Level")
        @Config.Comment("Maximum sky light level for spawning (0-15)")
        @Config.RangeInt(min = 0, max = 15)
        public int skyLightLevel = 8;

        @Config.Name("Block Light Level")
        @Config.Comment("Maximum block light level for spawning (0-15)")
        @Config.RangeInt(min = 0, max = 15)
        public int blockLightLevel = 0;
    }

    public static class Timers {
        @Config.Name("Time Until Spawn Min (Seconds)")
        @Config.Comment("Minimum time until next spawn attempt")
        @Config.RangeInt(min = 60, max = 3600)
        public int timeUntilSpawnMin = 300;

        @Config.Name("Time Until Spawn Max (Seconds)")
        @Config.Comment("Maximum time until next spawn attempt")
        @Config.RangeInt(min = 60, max = 3600)
        public int timeUntilSpawnMax = 600;

        @Config.Name("Cooldown After Spawn (Seconds)")
        @Config.Comment("Cooldown time after spawn")
        @Config.RangeInt(min = 60, max = 7200)
        public int cooldownAfterSpawn = 1200;

        @Config.Name("Cooldown Chance")
        @Config.Comment("Chance for cooldown timer to activate (0.0 - 1.0)")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double cooldownChance = 0.4D;

        @Config.Name("Time Until Leave (Seconds)")
        @Config.Comment("Time until Cave Dweller leaves on its own")
        @Config.RangeInt(min = 30, max = 1800)
        public int timeUntilLeave = 300;

        @Config.Name("Time Until Leave During Chase (Seconds)")
        @Config.Comment("Time until Cave Dweller leaves during chase")
        @Config.RangeInt(min = 5, max = 300)
        public int timeUntilLeaveChase = 30;

        @Config.Name("Reset Noise Min (Seconds)")
        @Config.Comment("Minimum time between cave noise sounds")
        @Config.RangeInt(min = 30, max = 600)
        public int resetNoiseMin = 240;

        @Config.Name("Reset Noise Max (Seconds)")
        @Config.Comment("Maximum time between cave noise sounds")
        @Config.RangeInt(min = 30, max = 600)
        public int resetNoiseMax = 360;
    }

    public static class Behavior {
        @Config.Name("Stare Time Min")
        @Config.Comment("Minimum amount of times it needs to be looked at to stop staring")
        @Config.RangeInt(min = 1, max = 20)
        public int stareTimeMin = 4;

        @Config.Name("Stare Time Max")
        @Config.Comment("Maximum amount of times it needs to be looked at to stop staring")
        @Config.RangeInt(min = 1, max = 30)
        public int stareTimeMax = 13;

        @Config.Name("Can Break Doors")
        @Config.Comment("Allow Cave Dweller to break doors")
        public boolean canBreakDoors = true;

        @Config.Name("Can Climb")
        @Config.Comment("Allow Cave Dweller to climb walls")
        public boolean canClimb = true;

        @Config.Name("Can Crawl")
        @Config.Comment("Allow Cave Dweller to crawl through 1-block gaps")
        public boolean canCrawl = true;

        @Config.Name("Attack Damage")
        @Config.Comment("Damage dealt by Cave Dweller")
        @Config.RangeDouble(min = 1.0D, max = 20.0D)
        public double attackDamage = 6.0D;

        @Config.Name("Max Health")
        @Config.Comment("Maximum health of Cave Dweller")
        @Config.RangeDouble(min = 10.0D, max = 200.0D)
        public double maxHealth = 60.0D;

        @Config.Name("Movement Speed")
        @Config.Comment("Movement speed of Cave Dweller")
        @Config.RangeDouble(min = 0.1D, max = 1.0D)
        public double movementSpeed = 0.3D;

        @Config.Name("Chase Speed")
        @Config.Comment("Movement speed during chase")
        @Config.RangeDouble(min = 0.1D, max = 1.5D)
        public double chaseSpeed = 0.5D;

        @Config.Name("Flee Speed")
        @Config.Comment("Movement speed during flee")
        @Config.RangeDouble(min = 0.1D, max = 1.5D)
        public double fleeSpeed = 0.4D;

        @Config.Name("Follow Range")
        @Config.Comment("Follow range of Cave Dweller")
        @Config.RangeDouble(min = 16.0D, max = 256.0D)
        public double followRange = 100.0D;

        @Config.Name("Spotting Range")
        @Config.Comment("Range at which Cave Dweller can spot players")
        @Config.RangeInt(min = 8, max = 128)
        public int spottingRange = 64;

        @Config.Name("Chase Time Until Leave")
        @Config.Comment("Time until Cave Dweller leaves during chase if player stops looking")
        @Config.RangeInt(min = 10, max = 120)
        public int chaseTimeUntilLeave = 30;
    }

    public static class Sound {
        @Config.Name("Cave Noise Volume")
        @Config.Comment("Volume of cave noise sounds")
        @Config.RangeDouble(min = 0.0D, max = 2.0D)
        public double caveNoiseVolume = 2.0D;

        @Config.Name("Cave Noise Pitch")
        @Config.Comment("Pitch of cave noise sounds")
        @Config.RangeDouble(min = 0.5D, max = 2.0D)
        public double caveNoisePitch = 1.0D;

        @Config.Name("Play Sounds At Player Position")
        @Config.Comment("Play ambient cave noises at player's position")
        public boolean playAtPlayerPosition = true;

        @Config.Name("Intensity Increases With Closer")
        @Config.Comment("Cave noise intensity increases as Cave Dweller gets closer")
        public boolean intensityIncreasesWithCloser = true;
    }

    public static class General {
        @Config.Name("Debug Mode")
        @Config.Comment("Enable debug mode for additional logging")
        public boolean debugMode = false;

        @Config.Name("Fast Forward")
        @Config.Comment("Fast forward timers for testing")
        public boolean fastForward = false;
    }

    @Mod.EventBusSubscriber(modid = CDLMain.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(CDLMain.MODID)) {
                ConfigManager.sync(CDLMain.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
