package com.b919.cave_dweller_legacy.common.command;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import com.b919.cave_dweller_legacy.entity.Roll;
import com.b919.cave_dweller_legacy.util.SpawnDirector;
import com.b919.cave_dweller_legacy.util.TargetUtil;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandCaveDweller extends CommandBase {
    @Override @MethodsReturnNonnullByDefault
    public String getName() {
        return "cavedweller";
    }

    @Override @MethodsReturnNonnullByDefault
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/cavedweller <forcespawn|get_state|get_target|get_timer|fast_forward> [args...]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        String subCommand = args[0];

        if ("forcespawn".equals(subCommand)) {
            handleForceSpawn(server, sender, args);
        } else if ("get_state".equals(subCommand)) {
            handleGetState(server, sender);
        } else if ("get_target".equals(subCommand)) {
            handleGetTarget(server, sender);
        } else if ("get_timer".equals(subCommand)) {
            handleGetTimer(server, sender);
        } else if ("fast_forward".equals(subCommand)) {
            handleFastForward(server, sender);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void handleForceSpawn(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1 && sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            spawnCaveDweller(player.getEntityWorld(), player.getPosition().add(0, 1, 0), player, sender, Roll.CHASE);
            return;
        }
        if (args.length == 2) {
            EntityPlayer target = getPlayer(server, sender, args[1]);
            EntityPlayer spawner = sender instanceof EntityPlayer ? (EntityPlayer) sender : target;
            Roll roll = Roll.CHASE;
            spawnCaveDweller(spawner.getEntityWorld(), spawner.getPosition().add(0, 1, 0), target, sender, roll);
            return;
        }
        if (args.length == 3) {
            EntityPlayer target = getPlayer(server, sender, args[1]);
            Roll roll = parseRoll(args[2]);
            EntityPlayer spawner = sender instanceof EntityPlayer ? (EntityPlayer) sender : target;
            spawnCaveDweller(spawner.getEntityWorld(), spawner.getPosition().add(0, 1, 0), target, sender, roll);
            return;
        }
        if (args.length >= 6) {
            BlockPos pos = parseBlockPos(sender, args, 1, false);
            EntityPlayer target = getPlayer(server, sender, args[5]);
            Roll roll = args.length >= 7 ? parseRoll(args[6]) : Roll.CHASE;
            spawnCaveDweller(sender.getEntityWorld(), pos, target, sender, roll);
            return;
        }
        throw new WrongUsageException("Usage: /cavedweller forcespawn [targetPlayer] [roll] OR /cavedweller forcespawn <x> <y> <z> <targetPlayer> [roll]");
    }

    private Roll parseRoll(String arg) {
        try {
            return Roll.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Roll.CHASE;
        }
    }

    private void spawnCaveDweller(net.minecraft.world.World world, BlockPos pos, EntityPlayer target, ICommandSender sender, Roll roll) {
        CaveDwellerEntity entity = new CaveDwellerEntity(world);
        entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setAttackTarget(target);
        entity.setInvisible(false);
        entity.currentRoll = roll;
        world.spawnEntity(entity);
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Force spawned Cave Dweller (" + roll.name() + ") targeting " + target.getName()));
    }

    private void handleGetState(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Must be run by a player"));
            return;
        }
        EntityPlayer player = (EntityPlayer) sender;
        List<CaveDwellerEntity> dwellers = player.getEntityWorld().getEntitiesWithinAABB(CaveDwellerEntity.class,
                new AxisAlignedBB(player.getPosition()).grow(50));
        if (dwellers.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No Cave Dwellers found within 50 blocks"));
            return;
        }
        for (CaveDwellerEntity dweller : dwellers) {
            String stateMsg = String.format("CaveDweller @ %s | Roll: %s | Invisible: %s | LifeTicks: %d | ChaseTicks: %d | Target: %s",
                    dweller.getPosition(),
                    dweller.currentRoll,
                    dweller.isInvisible(),
                    dweller.lifeTicks,
                    dweller.chaseTicks,
                    dweller.getAttackTarget() != null ? dweller.getAttackTarget().getName() : "none");
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + stateMsg));
        }
    }

    private void handleGetTarget(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Must be run by a player"));
            return;
        }
        EntityPlayer player = (EntityPlayer) sender;
        List<CaveDwellerEntity> dwellers = player.getEntityWorld().getEntitiesWithinAABB(CaveDwellerEntity.class,
                new AxisAlignedBB(player.getPosition()).grow(50));
        if (dwellers.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No Cave Dwellers found within 50 blocks"));
            return;
        }
        for (CaveDwellerEntity dweller : dwellers) {
            if (dweller.getAttackTarget() != null) {
                EntityPlayer target = (EntityPlayer) dweller.getAttackTarget();
                double dist = dweller.getDistance(target);
                boolean looking = target != null && TargetUtil.targetFacesMe(target, dweller);
                boolean canSee = target != null && TargetUtil.playerCanSeeMe(target, dweller);
                String targetMsg = String.format("CaveDweller @ %s -> Target: %s (dist: %.1f, looking: %s, canSee: %s)",
                        dweller.getPosition(), target.getName(), dist, looking, canSee);
                sender.sendMessage(new TextComponentString(TextFormatting.AQUA + targetMsg));
            } else {
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "CaveDweller @ " + dweller.getPosition() + " has no target"));
            }
        }
    }

    private void handleGetTimer(MinecraftServer server, ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Must be run by a player"));
            return;
        }
        EntityPlayer player = (EntityPlayer) sender;
        WorldServer world = (WorldServer) player.getEntityWorld();
        int dim = world.provider.getDimension();
        try {
            java.lang.reflect.Field statesField = SpawnDirector.class.getDeclaredField("states");
            statesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, SpawnDirector.DimensionState> states = (java.util.Map<Integer, SpawnDirector.DimensionState>) statesField.get(null);
            SpawnDirector.DimensionState state = states.get(dim);
            if (state == null) {
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No spawn state for dimension " + dim));
                return;
            }
            // Calculate next spawn attempt info
            String timerMsg = getTimerMsg(state, dim);
            sender.sendMessage(new TextComponentString(TextFormatting.AQUA + timerMsg));
            // Also show config values
            String configMsg = String.format("Config: spawnChance=%.3f minSpawn=%ds maxSpawn=%ds cooldownChance=%.1f cooldownAfter=%ds heightLimit=%d",
                    ModConfig.spawn.spawnChancePerTick,
                    ModConfig.timers.timeUntilSpawnMin,
                    ModConfig.timers.timeUntilSpawnMax,
                    ModConfig.timers.cooldownChance,
                    ModConfig.timers.cooldownAfterSpawn,
                    ModConfig.spawn.spawnHeightLimit);
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + configMsg));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Error reading spawn timers: " + e.getMessage()));
        }
    }

    @Nonnull
    private static String getTimerMsg(SpawnDirector.DimensionState state, int dim) {
        boolean canSpawn = state.spawnTimer <= 0 && state.aliveCount < ModConfig.spawn.maxSpawned;
        return String.format("Dimension %d | SpawnTimer: %ds | NoiseTimer: %ds | Cooldown: %ds | Alive: %d | InCooldown: %s | CanSpawn: %s | FastForward: %s",
                dim,
                state.spawnTimer / 20,
                state.noiseTimer / 20,
                state.cooldownTimer / 20,
                state.aliveCount,
                state.hasSpawnedInCooldown,
                canSpawn,
                ModConfig.general.fastForward);
    }

    private void handleFastForward(MinecraftServer server, ICommandSender sender) {
        ModConfig.general.fastForward = !ModConfig.general.fastForward;
        sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fast forward " + (ModConfig.general.fastForward ? "enabled" : "disabled")));
    }

    @Override @MethodsReturnNonnullByDefault
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> list = new ArrayList<String>();
        if (args.length == 1) {
            list.add("forcespawn");
            list.add("get_state");
            list.add("get_target");
            list.add("get_timer");
            list.add("fast_forward");
        } else if (args.length == 2 && "forcespawn".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else if (args.length == 3 && "forcespawn".equals(args[0])) {
            for (Roll r : Roll.values()) {
                list.add(r.name().toLowerCase());
            }
        } else if (args.length == 6 && "forcespawn".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        } else if (args.length == 7 && "forcespawn".equals(args[0])) {
            for (Roll r : Roll.values()) {
                list.add(r.name().toLowerCase());
            }
        }
        return list;
    }
}
