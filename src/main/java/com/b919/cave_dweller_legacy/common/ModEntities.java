package com.b919.cave_dweller_legacy.common;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class ModEntities {
    private static int entityId = 0;

    public static final ResourceLocation CAVE_DWELLER = new ResourceLocation(CDLMain.MODID, "cave_dweller");

    public static void register() {
        registerEntity(CaveDwellerEntity.class, "cave_dweller", 64, 1, true);
        EntityRegistry.registerEgg(CAVE_DWELLER, 0x2B2B2B, 0x9BE3E3);
    }

    private static void registerEntity(Class<? extends Entity> entityClass, String entityName, int trackingRange, int updateFrequency, boolean sendsVelocityUpdates) {
        EntityRegistry.registerModEntity(CAVE_DWELLER, entityClass, entityName, entityId++, CDLMain.instance, trackingRange, updateFrequency, sendsVelocityUpdates);
    }
}
