package com.b919.cave_dweller_legacy.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class CaveSoundPacket implements IMessage {
    public String soundKey;
    public double x;
    public double y;
    public double z;
    public float volume;
    public float pitch;

    public CaveSoundPacket() {
    }

    public CaveSoundPacket(String soundKey, double x, double y, double z, float volume, float pitch) {
        this.soundKey = soundKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.soundKey = ByteBufUtils.readUTF8String(buf);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.soundKey);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
    }
}
