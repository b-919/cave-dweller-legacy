package com.b919.cave_dweller_legacy.network;

import com.b919.cave_dweller_legacy.CDLMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CaveSoundPacketHandler implements IMessageHandler<CaveSoundPacket, IMessage> {
    @Override
    @SideOnly(Side.CLIENT)
    public IMessage onMessage(CaveSoundPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ResourceLocation location = new ResourceLocation(CDLMain.MODID, message.soundKey);
            PositionedSoundRecord sound = new PositionedSoundRecord(location, SoundCategory.AMBIENT,
                    message.volume, message.pitch, false, 0, ISound.AttenuationType.NONE,
                    (float) message.x, (float) message.y, (float) message.z);
            Minecraft.getMinecraft().getSoundHandler().playSound(sound);
        });
        return null;
    }
}
