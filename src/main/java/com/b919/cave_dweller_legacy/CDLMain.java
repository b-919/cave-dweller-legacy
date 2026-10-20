package com.b919.cave_dweller_legacy;

import com.b919.cave_dweller_legacy.common.CommonProxy;
import com.b919.cave_dweller_legacy.common.ModEntities;
import com.b919.cave_dweller_legacy.common.ModSounds;
import com.b919.cave_dweller_legacy.common.config.ModConfig;
import com.b919.cave_dweller_legacy.event.WorldEvents;
import com.b919.cave_dweller_legacy.common.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = CDLMain.MODID, name = CDLMain.NAME, version = CDLMain.VERSION)
public class CDLMain {
    public static final String MODID = "cave_dweller_legacy";
    public static final String NAME = "Cave Dweller Legacy";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @Instance(MODID)
    public static CDLMain instance;

    @SidedProxy(clientSide = "com.b919.cave_dweller_legacy.client.ClientProxy", serverSide = "com.b919.cave_dweller_legacy.common.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
