package com.b919.cave_dweller_legacy.common;

import com.b919.cave_dweller_legacy.event.WorldEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ModEntities.register();
        ModNetwork.init();
        MinecraftForge.EVENT_BUS.register(new WorldEvents());
    }

    public void init(FMLInitializationEvent event) {
    }
}