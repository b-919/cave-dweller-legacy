package com.b919.cave_dweller_legacy.util;

import com.b919.cave_dweller_legacy.common.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DwellerLiquid {
    private static final String[] EMPTY_ENTRIES = new String[0];
    private static final LiquidInfo WATER = new LiquidInfo(Kind.WATER, "", 0.0D, 0.0D, 0.0F, 0.0F);
    private static final LiquidInfo LAVA = new LiquidInfo(Kind.LAVA, "", 0.0D, 0.0D, 0.0F, 0.0F);

    private static volatile String[] cachedEntries;
    private static volatile Map<String, LiquidInfo> customLiquids = Collections.emptyMap();

    private DwellerLiquid() {
    }

    @Nullable
    public static LiquidInfo get(IBlockAccess world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }

        IBlockState state = world.getBlockState(pos);
        Fluid fluid = getFluid(state.getBlock());
        if (fluid == FluidRegistry.WATER) {
            return WATER;
        }
        if (fluid == FluidRegistry.LAVA) {
            return LAVA;
        }
        LiquidInfo custom;
        if (fluid != null) {
            custom = findCustom(fluid);
        } else {
            custom = findCustomByBlock(state.getBlock());
        }
        if (custom != null) {
            return custom;
        }

        Material material = state.getMaterial();
        if (material == Material.WATER) {
            return WATER;
        }
        if (material == Material.LAVA) {
            return LAVA;
        }
        return null;
    }

    @Nullable
    public static LiquidInfo findInEntity(IBlockAccess world, AxisAlignedBB box) {
        if (world == null || box == null) {
            return null;
        }

        AxisAlignedBB bounds = box.shrink(0.001D);
        int minX = MathHelper.floor(bounds.minX);
        int minY = MathHelper.floor(bounds.minY);
        int minZ = MathHelper.floor(bounds.minZ);
        int maxX = MathHelper.floor(bounds.maxX - 1.0E-7D);
        int maxY = MathHelper.floor(bounds.maxY - 1.0E-7D);
        int maxZ = MathHelper.floor(bounds.maxZ - 1.0E-7D);
        LiquidInfo first = null;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    LiquidInfo liquid = get(world, new BlockPos(x, y, z));
                    if (liquid == null) {
                        continue;
                    }
                    if (first == null) {
                        first = liquid;
                    }
                    if (y == minY) {
                        return liquid;
                    }
                }
            }
        }
        return first;
    }

    public static void invalidate() {
        synchronized (DwellerLiquid.class) {
            cachedEntries = null;
            customLiquids = Collections.emptyMap();
        }
    }

    @Nullable
    private static LiquidInfo findCustom(Fluid fluid) {
        Map<String, LiquidInfo> map = getCustomLiquids();
        LiquidInfo info = map.get(normalize(fluid.getName()));
        if (info != null) {
            return info;
        }
        return findCustomByBlock(fluid.getBlock());
    }

    @Nullable
    private static LiquidInfo findCustomByBlock(Block block) {
        if (block == null || block.getRegistryName() == null) {
            return null;
        }
        Map<String, LiquidInfo> map = getCustomLiquids();
        return map.get(normalize(block.getRegistryName().toString()));
    }

    private static Map<String, LiquidInfo> getCustomLiquids() {
        String[] entries = ModConfig.swimming == null || ModConfig.swimming.moddedLiquids == null
                ? EMPTY_ENTRIES : ModConfig.swimming.moddedLiquids;
        if (entries != cachedEntries) {
            synchronized (DwellerLiquid.class) {
                if (entries != cachedEntries) {
                    customLiquids = parse(entries);
                    cachedEntries = entries;
                }
            }
        }
        return customLiquids;
    }

    private static Map<String, LiquidInfo> parse(String[] entries) {
        Map<String, LiquidInfo> liquids = new HashMap<>();
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }

            String[] override = entry.split("=", 2);
            String name = normalize(override[0]);
            if (name.isEmpty()) {
                continue;
            }

            double upSpeed = ModConfig.swimming.moddedLiquidUpSpeed;
            double downSpeed = ModConfig.swimming.moddedLiquidDownSpeed;
            float surfaceCost = (float) ModConfig.swimming.moddedLiquidSurfaceCost;
            float submergedCost = (float) ModConfig.swimming.moddedLiquidSubmergedCost;
            if (override.length > 1) {
                String[] values = override[1].split(",");
                upSpeed = parseDouble(values, 0, upSpeed);
                downSpeed = parseDouble(values, 1, downSpeed);
                surfaceCost = (float) parseDouble(values, 2, surfaceCost);
                submergedCost = (float) parseDouble(values, 3, submergedCost);
            }

            LiquidInfo info = new LiquidInfo(Kind.MODDED, name,
                    clamp(upSpeed, 0.0D, 0.2D), clamp(downSpeed, 0.0D, 0.2D),
                    (float) clamp(surfaceCost, 0.0D, 64.0D), (float) clamp(submergedCost, 0.0D, 64.0D));
            liquids.put(name, info);

            int colon = name.indexOf(':');
            if (colon >= 0 && colon + 1 < name.length()) {
                liquids.put(name.substring(colon + 1), info);
            }
        }
        return liquids.isEmpty() ? Collections.emptyMap() : liquids;
    }

    private static double parseDouble(String[] values, int index, double fallback) {
        if (index >= values.length || values[index].trim().isEmpty()) {
            return fallback;
        }
        try {
            double value = Double.parseDouble(values[index].trim());
            return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
        } catch (NumberFormatException exception) {
            if (ModConfig.general != null && ModConfig.general.debugMode) {
                System.out.println("[CAVE_DWELLER_DEBUG] Invalid liquid parameter '" + values[index] + "'");
            }
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    private static Fluid getFluid(Block block) {
        if (block == null) {
            return null;
        }
        if (block instanceof IFluidBlock) {
            Fluid fluid = ((IFluidBlock) block).getFluid();
            if (fluid != null) {
                return fluid;
            }
        }
        return FluidRegistry.lookupFluidForBlock(block);
    }

    private enum Kind {
        WATER,
        LAVA,
        MODDED
    }

    public static final class LiquidInfo {
        private final Kind kind;
        private final String name;
        private final double upSpeed;
        private final double downSpeed;
        private final float surfaceCost;
        private final float submergedCost;

        private LiquidInfo(Kind kind, String name, double upSpeed, double downSpeed,
                           float surfaceCost, float submergedCost) {
            this.kind = kind;
            this.name = name;
            this.upSpeed = upSpeed;
            this.downSpeed = downSpeed;
            this.surfaceCost = surfaceCost;
            this.submergedCost = submergedCost;
        }

        public boolean isWater() {
            return this.kind == Kind.WATER;
        }

        public boolean isLava() {
            return this.kind == Kind.LAVA;
        }

        public boolean isModded() {
            return this.kind == Kind.MODDED;
        }

        public double getUpSpeed() {
            if (this.kind == Kind.WATER) {
                return ModConfig.swimming.waterUpSpeed;
            }
            if (this.kind == Kind.LAVA) {
                return ModConfig.swimming.lavaUpSpeed;
            }
            return this.upSpeed;
        }

        public double getDownSpeed() {
            if (this.kind == Kind.WATER) {
                return ModConfig.swimming.waterDownSpeed;
            }
            if (this.kind == Kind.LAVA) {
                return ModConfig.swimming.lavaDownSpeed;
            }
            return this.downSpeed;
        }

        public float getSurfaceCost() {
            if (this.kind == Kind.WATER) {
                return (float) ModConfig.swimming.waterSurfaceCost;
            }
            if (this.kind == Kind.LAVA) {
                return 0.0F;
            }
            return this.surfaceCost;
        }

        public float getSubmergedCost() {
            if (this.kind == Kind.WATER) {
                return (float) ModConfig.swimming.waterSubmergedCost;
            }
            if (this.kind == Kind.LAVA) {
                return 0.0F;
            }
            return this.submergedCost;
        }

        public boolean matches(IBlockAccess world, BlockPos pos) {
            return get(world, pos) == this;
        }

        public String getName() {
            return this.name;
        }
    }
}
