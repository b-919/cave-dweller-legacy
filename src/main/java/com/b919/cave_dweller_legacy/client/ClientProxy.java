package com.b919.cave_dweller_legacy.client;

import com.b919.cave_dweller_legacy.client.render.RenderCaveDweller;
import com.b919.cave_dweller_legacy.common.CommonProxy;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(CaveDwellerEntity.class, RenderCaveDweller::new);
    }
}
