package com.b919.cave_dweller_legacy.common;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.network.CaveSoundPacket;
import com.b919.cave_dweller_legacy.network.CaveSoundPacketHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ModNetwork {
    public static SimpleNetworkWrapper INSTANCE;

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(CDLMain.MODID + ":net");
        INSTANCE.registerMessage(CaveSoundPacketHandler.class, CaveSoundPacket.class, 0, Side.CLIENT);
    }
}
