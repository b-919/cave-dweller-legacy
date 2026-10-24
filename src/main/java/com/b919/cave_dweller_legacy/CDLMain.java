package com.b919.cave_dweller_legacy;

import com.b919.cave_dweller_legacy.common.CommonProxy;
import software.bernie.geckolib3.GeckoLib;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = CDLMain.MODID, name = CDLMain.NAME, version = CDLMain.VERSION, dependencies = "required-after:geckolib3")
public class CDLMain {
    public static final String MODID = "cave_dweller_legacy";
    public static final String NAME = "Cave Dweller Legacy";
    public static final String VERSION = "1.0";

    @Instance(MODID)
    public static CDLMain instance;

    @SidedProxy(clientSide = "com.b919.cave_dweller_legacy.client.ClientProxy", serverSide = "com.b919.cave_dweller_legacy.common.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GeckoLib.initialize();
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new com.b919.cave_dweller_legacy.common.command.CommandCaveDweller());
    }
}
