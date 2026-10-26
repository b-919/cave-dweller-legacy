package com.b919.cave_dweller_legacy.common.config;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.util.DwellerLiquid;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = CDLMain.MODID)
@Config.LangKey("config.cave_dweller_legacy.title")
public class ModConfig {
    @Config.LangKey("config.cave_dweller_legacy.category.spawn")
    @Config.Name("Spawn")
    public static Spawn spawn = new Spawn();

    @Config.LangKey("config.cave_dweller_legacy.category.timers")
    @Config.Name("Timers")
    public static Timers timers = new Timers();

    @Config.LangKey("config.cave_dweller_legacy.category.behavior")
    @Config.Name("Behavior")
    public static Behavior behavior = new Behavior();

    @Config.LangKey("config.cave_dweller_legacy.category.swimming")
    @Config.Name("Swimming")
    public static Swimming swimming = new Swimming();

    @Config.LangKey("config.cave_dweller_legacy.category.leaping")
    @Config.Name("Leaping")
    public static Leaping leaping = new Leaping();

    @Config.LangKey("config.cave_dweller_legacy.category.grief")
    @Config.Name("Grief Mode")
    public static Grief grief = new Grief();

    @Config.LangKey("config.cave_dweller_legacy.category.sound")
    @Config.Name("Sound")
    public static Sound sound = new Sound();

    @Config.LangKey("config.cave_dweller_legacy.category.general")
    @Config.Name("General")
    public static General general = new General();

    public static class Spawn {
        @Config.LangKey("config.cave_dweller_legacy.spawn.spawn_chance_per_tick")
        @Config.Name("Spawn Chance Per Tick")
        @Config.Comment("Chance per tick to attempt spawning when timer expires (0.0 - 1.0)")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public float spawnChancePerTick = 0.005F;

        @Config.LangKey("config.cave_dweller_legacy.spawn.spawn_height_limit")
        @Config.Name("Spawn Height Limit")
        @Config.Comment("Maximum Y level to spawn below")
        @Config.RangeInt(min = 0, max = 256)
        public int spawnHeightLimit = 40;

        @Config.LangKey("config.cave_dweller_legacy.spawn.allow_surface_spawn")
        @Config.Name("Allow Surface Spawn")
        @Config.Comment("Allow spawning on the surface (not just underground)")
        public boolean allowSurfaceSpawn = false;

        @Config.LangKey("config.cave_dweller_legacy.spawn.max_spawned")
        @Config.Name("Max Spawned")
        @Config.Comment("Maximum number of Cave Dwellers alive at once")
        @Config.RangeInt(min = 1, max = 10)
        public int maxSpawned = 1;

        @Config.LangKey("config.cave_dweller_legacy.spawn.spawn_distance")
        @Config.Name("Spawn Distance From Player")
        @Config.Comment("Maximum distance to spawn from player")
        @Config.RangeInt(min = 16, max = 128)
        public int spawnDistance = 64;

        @Config.LangKey("config.cave_dweller_legacy.spawn.min_spawn_distance_from_player")
        @Config.Name("Min Spawn Distance From Player")
        @Config.Comment("Minimum distance to spawn from player")
        @Config.RangeInt(min = 4, max = 32)
        public int minSpawnDistanceFromPlayer = 16;

        //@Config.Name("Check Path To Spawn")
        //@Config.Comment("Check if there is a path from spawn location to player")
        //public boolean checkPathToSpawn = true;

        @Config.LangKey("config.cave_dweller_legacy.spawn.dimension_whitelist")
        @Config.Name("Dimension Whitelist")
        @Config.Comment("Dimensions where Cave Dweller can spawn (e.g., 0 for Overworld)")
        public int[] dimensionWhitelist = new int[]{0};

        @Config.LangKey("config.cave_dweller_legacy.spawn.sky_light_level")
        @Config.Name("Sky Light Level")
        @Config.Comment("Maximum sky light level for spawning (0-15)")
        @Config.RangeInt(min = 0, max = 15)
        public int skyLightLevel = 8;

        /*@Config.Name("Block Light Level")
        @Config.Comment("Maximum block light level for spawning (0-15)")
        @Config.RangeInt(min = 0, max = 15)
        public int blockLightLevel = 0;*/
    }

    public static class Timers {
        @Config.LangKey("config.cave_dweller_legacy.timers.time_until_spawn_min")
        @Config.Name("Time Until Spawn Min (Seconds)")
        @Config.Comment("Minimum time until next spawn attempt")
        @Config.RangeInt(min = 60, max = 3600)
        public int timeUntilSpawnMin = 60;

        @Config.LangKey("config.cave_dweller_legacy.timers.time_until_spawn_max")
        @Config.Name("Time Until Spawn Max (Seconds)")
        @Config.Comment("Maximum time until next spawn attempt")
        @Config.RangeInt(min = 60, max = 3600)
        public int timeUntilSpawnMax = 120;

        @Config.LangKey("config.cave_dweller_legacy.timers.cooldown_after_spawn")
        @Config.Name("Cooldown After Spawn (Seconds)")
        @Config.Comment("Cooldown time after spawn")
        @Config.RangeInt(min = 60, max = 7200)
        public int cooldownAfterSpawn = 180;

        @Config.LangKey("config.cave_dweller_legacy.timers.cooldown_chance")
        @Config.Name("Cooldown Chance")
        @Config.Comment("Chance for cooldown timer to activate (0.0 - 1.0)")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double cooldownChance = 0.4D;

        @Config.LangKey("config.cave_dweller_legacy.timers.time_until_leave")
        @Config.Name("Time Until Leave (Seconds)")
        @Config.Comment("Time until Cave Dweller leaves on its own")
        @Config.RangeInt(min = 30, max = 1800)
        public int timeUntilLeave = 300;

        @Config.LangKey("config.cave_dweller_legacy.timers.reset_noise_min")
        @Config.Name("Reset Noise Min (Seconds)")
        @Config.Comment("Minimum time between cave noise sounds")
        @Config.RangeInt(min = 30, max = 600)
        public int resetNoiseMin = 60;

        @Config.LangKey("config.cave_dweller_legacy.timers.reset_noise_max")
        @Config.Name("Reset Noise Max (Seconds)")
        @Config.Comment("Maximum time between cave noise sounds")
        @Config.RangeInt(min = 30, max = 600)
        public int resetNoiseMax = 120;
    }

    public static class Behavior {
        @Config.LangKey("config.cave_dweller_legacy.behavior.stare_time_min")
        @Config.Name("Stare Time Min")
        @Config.Comment("Minimum amount of times it needs to be looked at before it can stop staring")
        @Config.RangeInt(min = 1, max = 20)
        public int stareTimeMin = 2;

        @Config.LangKey("config.cave_dweller_legacy.behavior.stare_time_max")
        @Config.Name("Stare Time Max")
        @Config.Comment("Maximum amount of times it needs to be looked at before it can stop staring")
        @Config.RangeInt(min = 1, max = 30)
        public int stareTimeMax = 5;

        @Config.LangKey("config.cave_dweller_legacy.behavior.stare_hold_ticks")
        @Config.Name("Stare Hold Ticks")
        @Config.Comment("Ticks of continuous staring that count as one additional look")
        @Config.RangeInt(min = 10, max = 200)
        public int stareHoldTicks = 40;

        @Config.LangKey("config.cave_dweller_legacy.behavior.proximity_chase_range")
        @Config.Name("Proximity Chase Range")
        @Config.Comment("Distance in blocks at which the dweller may enter chase on its own. Set to 0 to disable")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double proximityChaseRange = 8.0D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.proximity_chase_chance")
        @Config.Name("Proximity Chase Chance")
        @Config.Comment("Chance for a proximity check to start a chase when a player is in range (0.0 - 1.0)")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double proximityChaseChance = 1.0D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.chase_roll_interval_ticks")
        @Config.Name("Chase Roll Interval (Ticks)")
        @Config.Comment("Minimum ticks between chase-entry rolls for proximity and stare checks. Lower is faster")
        @Config.RangeInt(min = 1, max = 200)
        public int chaseRollIntervalTicks = 10;

        @Config.LangKey("config.cave_dweller_legacy.behavior.can_climb")
        @Config.Name("Can Climb")
        @Config.Comment("Allow Cave Dweller to climb walls")
        public boolean canClimb = true;

        @Config.LangKey("config.cave_dweller_legacy.behavior.can_crawl")
        @Config.Name("Can Crawl")
        @Config.Comment("Allow Cave Dweller to crawl through 1-block gaps")
        public boolean canCrawl = true;

        @Config.LangKey("config.cave_dweller_legacy.behavior.attack_damage")
        @Config.Name("Attack Damage")
        @Config.Comment("Damage dealt by Cave Dweller")
        @Config.RangeDouble(min = 1.0D, max = 20.0D)
        public double attackDamage = 6.0D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.max_health")
        @Config.Name("Max Health")
        @Config.Comment("Maximum health of Cave Dweller")
        @Config.RangeDouble(min = 10.0D, max = 200.0D)
        public double maxHealth = 60.0D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.movement_speed")
        @Config.Name("Movement Speed")
        @Config.Comment("Movement speed of Cave Dweller")
        @Config.RangeDouble(min = 0.1D, max = 1.0D)
        public double movementSpeed = 0.95D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.chase_speed")
        @Config.Name("Chase Speed")
        @Config.Comment("Movement speed multiplier during chase")
        @Config.RangeDouble(min = 0.1D, max = 1.5D)
        public double chaseSpeed = 0.87D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.attack_reach")
        @Config.Name("Attack Reach")
        @Config.Comment("Attack reach distance in blocks")
        @Config.RangeDouble(min = 1.0D, max = 6.0D)
        public double attackReach = 2.15D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.flee_speed")
        @Config.Name("Flee Speed")
        @Config.Comment("Movement speed multiplier during flee")
        @Config.RangeDouble(min = 0.1D, max = 1.5D)
        public double fleeSpeed = 1.0D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.stroll_speed")
        @Config.Name("Stroll Speed")
        @Config.Comment("Movement speed multiplier while strolling")
        @Config.RangeDouble(min = 0.1D, max = 1.5D)
        public double strollSpeed = 0.7D;

        @Config.LangKey("config.cave_dweller_legacy.behavior.follow_range")
        @Config.Name("Follow Range")
        @Config.Comment("Follow range of Cave Dweller")
        @Config.RangeDouble(min = 16.0D, max = 256.0D)
        public double followRange = 100.0D;

        /*@Config.Name("Spotting Range")
        @Config.Comment("Range at which Cave Dweller can spot players")
        @Config.RangeInt(min = 8, max = 128)
        public int spottingRange = 100;*/

        @Config.LangKey("config.cave_dweller_legacy.behavior.chase_time_until_leave")
        @Config.Name("Chase Time Until Leave")
        @Config.Comment("Time until Cave Dweller leaves during chase if player stops looking")
        @Config.RangeInt(min = 10, max = 120)
        public int chaseTimeUntilLeave = 30;
    }

    public static class Swimming {
        @Config.LangKey("config.cave_dweller_legacy.swimming.can_swim")
        @Config.Name("Can Swim")
        @Config.Comment("Allow the Cave Dweller to float and swim in water, lava, and listed modded liquids")
        public boolean canSwim = true;

        @Config.LangKey("config.cave_dweller_legacy.swimming.water_up_speed")
        @Config.Name("Water Up Speed")
        @Config.Comment("Maximum upward swim speed in water, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double waterUpSpeed = 0.06D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.water_down_speed")
        @Config.Name("Water Down Speed")
        @Config.Comment("Maximum downward swim speed in water, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double waterDownSpeed = 0.05D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.lava_up_speed")
        @Config.Name("Lava Up Speed")
        @Config.Comment("Maximum upward swim speed in lava, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double lavaUpSpeed = 0.05D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.lava_down_speed")
        @Config.Name("Lava Down Speed")
        @Config.Comment("Maximum downward swim speed in lava, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double lavaDownSpeed = 0.04D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.vertical_response")
        @Config.Name("Vertical Response")
        @Config.Comment("How strongly vertical speed reacts to the height difference from the target")
        @Config.RangeDouble(min = 0.05D, max = 1.0D)
        public double verticalResponse = 0.22D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.vertical_dead_zone")
        @Config.Name("Vertical Dead Zone")
        @Config.Comment("Height difference treated as on-target, preventing surface bobbing")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double verticalDeadZone = 0.1D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.max_ledge_boost")
        @Config.Name("Max Ledge Boost")
        @Config.Comment("Maximum upward boost preserved from vanilla when pressing against a liquid bank")
        @Config.RangeDouble(min = 0.0D, max = 0.6D)
        public double maxLedgeBoost = 0.3D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.minimum_swim_depth")
        @Config.Name("Minimum Swim Depth")
        @Config.Comment("Liquid depth required to start swimming while standing on the bottom")
        @Config.RangeInt(min = 1, max = 16)
        public int minimumSwimDepth = 3;

        @Config.LangKey("config.cave_dweller_legacy.swimming.water_surface_cost")
        @Config.Name("Water Surface Cost")
        @Config.Comment("Pathfinding cost of water nodes whose head is above the surface")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double waterSurfaceCost = 4.0D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.water_submerged_cost")
        @Config.Name("Water Submerged Cost")
        @Config.Comment("Pathfinding cost of water nodes whose head is submerged")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double waterSubmergedCost = 8.0D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.modded_liquids")
        @Config.Name("Modded Liquids")
        @Config.Comment("Fluid names the Cave Dweller can swim in, for example crude_oil or thermalfoundation:crude_oil. " +
                "Add optional overrides with =upSpeed,downSpeed,surfaceCost,submergedCost after the name")
        public String[] moddedLiquids = new String[0];

        @Config.LangKey("config.cave_dweller_legacy.swimming.modded_liquid_up_speed")
        @Config.Name("Modded Liquid Up Speed")
        @Config.Comment("Default maximum upward swim speed in listed modded liquids, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double moddedLiquidUpSpeed = 0.06D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.modded_liquid_down_speed")
        @Config.Name("Modded Liquid Down Speed")
        @Config.Comment("Default maximum downward swim speed in listed modded liquids, in blocks per tick")
        @Config.RangeDouble(min = 0.0D, max = 0.2D)
        public double moddedLiquidDownSpeed = 0.05D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.modded_liquid_surface_cost")
        @Config.Name("Modded Liquid Surface Cost")
        @Config.Comment("Default pathfinding cost of listed modded liquid nodes whose head is above the surface")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double moddedLiquidSurfaceCost = 4.0D;

        @Config.LangKey("config.cave_dweller_legacy.swimming.modded_liquid_submerged_cost")
        @Config.Name("Modded Liquid Submerged Cost")
        @Config.Comment("Default pathfinding cost of listed modded liquid nodes whose head is submerged")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double moddedLiquidSubmergedCost = 8.0D;
    }

    public static class Leaping {
        @Config.LangKey("config.cave_dweller_legacy.leaping.enabled")
        @Config.Name("Enabled")
        @Config.Comment("Allow the Cave Dweller to leap across gaps, bridges, and parkour routes")
        public boolean enabled = true;

        @Config.LangKey("config.cave_dweller_legacy.leaping.max_leap_distance")
        @Config.Name("Max Leap Distance")
        @Config.Comment("Maximum center-to-center leap distance in blocks. 4.0 crosses a 3-block air gap")
        @Config.RangeDouble(min = 1.0D, max = 64.0D)
        public double maxLeapDistance = 4.0D;

        @Config.LangKey("config.cave_dweller_legacy.leaping.max_leap_rise")
        @Config.Name("Max Leap Rise")
        @Config.Comment("Maximum blocks a leap may climb to reach a higher ledge")
        @Config.RangeInt(min = 0, max = 16)
        public int maxLeapRise = 2;

        @Config.LangKey("config.cave_dweller_legacy.leaping.max_leap_drop")
        @Config.Name("Max Leap Drop")
        @Config.Comment("Maximum blocks a leap may descend to reach a lower ledge")
        @Config.RangeInt(min = 0, max = 32)
        public int maxLeapDrop = 4;

        @Config.LangKey("config.cave_dweller_legacy.leaping.leap_speed")
        @Config.Name("Leap Speed")
        @Config.Comment("Target horizontal speed of a leap in blocks per tick. Leaps are spread over whole ticks; long leaps use a higher speed")
        @Config.RangeDouble(min = 0.1D, max = 4.0D)
        public double leapSpeed = 0.5D;

        @Config.LangKey("config.cave_dweller_legacy.leaping.max_leap_ticks")
        @Config.Name("Max Leap Ticks")
        @Config.Comment("Maximum ticks spent airborne during one leap")
        @Config.RangeInt(min = 5, max = 200)
        public int maxLeapTicks = 40;

        @Config.LangKey("config.cave_dweller_legacy.leaping.leap_path_cost")
        @Config.Name("Leap Path Cost")
        @Config.Comment("Extra pathfinding cost added to a leap, so normal walking is preferred when possible")
        @Config.RangeDouble(min = 0.0D, max = 64.0D)
        public double leapPathCost = 4.0D;

        @Config.LangKey("config.cave_dweller_legacy.leaping.leap_cooldown")
        @Config.Name("Leap Cooldown")
        @Config.Comment("Ticks the Cave Dweller waits after landing before it may leap again")
        @Config.RangeInt(min = 0, max = 100)
        public int leapCooldown = 3;
    }

    public static class Grief {
        @Config.LangKey("config.cave_dweller_legacy.grief.enabled")
        @Config.Name("Enabled")
        @Config.Comment("Allow the Cave Dweller to break blocks when it cannot reach a player")
        public boolean enabled = false;

        @Config.LangKey("config.cave_dweller_legacy.grief.max_block_hardness")
        @Config.Name("Maximum Block Hardness")
        @Config.Comment("Maximum hardness the Cave Dweller can break unless a block is whitelisted")
        @Config.RangeDouble(min = 0.0D, max = 100.0D)
        public double maxBlockHardness = 2.0D;

        @Config.LangKey("config.cave_dweller_legacy.grief.block_blacklist")
        @Config.Name("Block Blacklist")
        @Config.Comment("Registry names that can never be broken, for example minecraft:bedrock; blacklist takes priority")
        public String[] blockBlacklist = new String[0];

        @Config.LangKey("config.cave_dweller_legacy.grief.block_whitelist")
        @Config.Name("Block Whitelist")
        @Config.Comment("Registry names that may be broken regardless of the maximum hardness")
        public String[] blockWhitelist = new String[0];

        @Config.LangKey("config.cave_dweller_legacy.grief.block_break_speed")
        @Config.Name("Block Break Speed")
        @Config.Comment("Break speed multiplier; higher values break blocks faster")
        @Config.RangeDouble(min = 0.1D, max = 10.0D)
        public double blockBreakSpeed = 1.0D;

        @Config.LangKey("config.cave_dweller_legacy.grief.drop_broken_blocks")
        @Config.Name("Drop Broken Blocks")
        @Config.Comment("Drop block items when the Cave Dweller breaks a block")
        public boolean dropBrokenBlocks = true;

        @Config.LangKey("config.cave_dweller_legacy.grief.unreachable_delay_ticks")
        @Config.Name("Unreachable Delay (Ticks)")
        @Config.Comment("How long a chase target must remain unreachable before the Cave Dweller starts breaking blocks")
        @Config.RangeInt(min = 0, max = 1200)
        public int unreachableDelayTicks = 40;

        @Config.LangKey("config.cave_dweller_legacy.grief.tunnel_carver_mode")
        @Config.Name("Tunnel Carver Mode")
        @Config.Comment("Let the Cave Dweller move toward nearby players and carve obstructing blocks outside chase mode")
        public boolean tunnelCarverMode = false;

        @Config.LangKey("config.cave_dweller_legacy.grief.tunnel_range")
        @Config.Name("Tunnel Range")
        @Config.Comment("Maximum distance to a player for tunnel-carver behavior")
        @Config.RangeInt(min = 8, max = 128)
        public int tunnelRange = 32;

        @Config.LangKey("config.cave_dweller_legacy.grief.tunnel_stop_distance")
        @Config.Name("Tunnel Stop Distance")
        @Config.Comment("Stop tunnel-carver behavior this close to the player")
        @Config.RangeInt(min = 1, max = 16)
        public int tunnelStopDistance = 4;
    }

    public static class Sound {
        @Config.LangKey("config.cave_dweller_legacy.sound.cave_noise_volume")
        @Config.Name("Cave Noise Volume")
        @Config.Comment("Volume of cave noise sounds")
        @Config.RangeDouble(min = 0.0D, max = 2.0D)
        public double caveNoiseVolume = 2.0D;

        @Config.LangKey("config.cave_dweller_legacy.sound.cave_noise_pitch")
        @Config.Name("Cave Noise Pitch")
        @Config.Comment("Pitch of cave noise sounds")
        @Config.RangeDouble(min = 0.5D, max = 2.0D)
        public double caveNoisePitch = 1.0D;

        @Config.LangKey("config.cave_dweller_legacy.sound.play_at_player_position")
        @Config.Name("Play Sounds At Player Position")
        @Config.Comment("Play ambient cave noises at player's position")
        public boolean playAtPlayerPosition = true;
    }

    public static class General {
        @Config.LangKey("config.cave_dweller_legacy.general.debug_mode")
        @Config.Name("Debug Mode")
        @Config.Comment("Enable debug mode for additional logging")
        public boolean debugMode = false;

        @Config.LangKey("config.cave_dweller_legacy.general.fast_forward")
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
                DwellerLiquid.invalidate();
            }
        }
    }
}
