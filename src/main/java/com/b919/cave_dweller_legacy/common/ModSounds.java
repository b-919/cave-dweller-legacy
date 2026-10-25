package com.b919.cave_dweller_legacy.common;

import com.b919.cave_dweller_legacy.CDLMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CDLMain.MODID)
public class ModSounds {
    public static SoundEvent CAVE_NOISE;
    public static SoundEvent CHASE;
    public static SoundEvent CHASE_STEP;
    public static SoundEvent HURT;
    public static SoundEvent FLEE;
    public static SoundEvent DEATH;
    public static SoundEvent SPOTTED;
    public static SoundEvent DISAPPEAR;

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        CAVE_NOISE = registerSound(event, "cavenoise");
        CHASE = registerSound(event, "chase");
        CHASE_STEP = registerSound(event, "chase_step");
        HURT = registerSound(event, "dweller_hurt");
        FLEE = registerSound(event, "flee");
        DEATH = registerSound(event, "death");
        SPOTTED = registerSound(event, "spotted");
        DISAPPEAR = registerSound(event, "disappear");
    }

    private static SoundEvent registerSound(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation location = new ResourceLocation(CDLMain.MODID, name);
        SoundEvent soundEvent = new SoundEvent(location).setRegistryName(location);
        event.getRegistry().register(soundEvent);
        return soundEvent;
    }
}
