package com.b919.cave_dweller_legacy.util;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;

public final class GriefUtil {
    private GriefUtil() {
    }

    public static boolean canBreak(World world, BlockPos pos, IBlockState state) {
        if (world == null || pos == null || state == null) {
            return false;
        }

        if (state.getBlock().isAir(state, world, pos) || state.getMaterial().isLiquid()
                || state.getBlock().hasTileEntity(state)) {
            return false;
        }

        float hardness = state.getBlockHardness(world, pos);
        if (hardness < 0.0F) {
            return false;
        }

        String registryName = state.getBlock().getRegistryName() == null
                ? "" : state.getBlock().getRegistryName().toString().toLowerCase(Locale.ROOT);
        if (contains(ModConfig.grief.blockBlacklist, registryName)) {
            return false;
        }
        if (contains(ModConfig.grief.blockWhitelist, registryName)) {
            return true;
        }
        return hardness <= ModConfig.grief.maxBlockHardness;
    }

    private static boolean contains(String[] names, String registryName) {
        if (names == null || registryName.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (name != null && registryName.equals(name.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
