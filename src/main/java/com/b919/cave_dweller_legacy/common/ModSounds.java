package com.b919.cave_dweller_legacy.common;

import com.b919.cave_dweller_legacy.CDLMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = CDLMain.MODID)
public class ModSounds {
    public static SoundEvent CAVE_NOISE_1;
    public static SoundEvent CAVE_NOISE_2;
    public static SoundEvent CAVE_NOISE_3;
    public static SoundEvent CAVE_NOISE_4;
    public static SoundEvent CHASE_1;
    public static SoundEvent CHASE_2;
    public static SoundEvent CHASE_3;
    public static SoundEvent CHASE_4;
    public static SoundEvent FLEE_1;
    public static SoundEvent FLEE_2;
    public static SoundEvent HURT_1;
    public static SoundEvent HURT_2;
    public static SoundEvent HURT_3;
    public static SoundEvent HURT_4;
    public static SoundEvent DEATH;
    public static SoundEvent STEPS_1;
    public static SoundEvent STEPS_2;
    public static SoundEvent STEPS_3;
    public static SoundEvent STEPS_4;
    public static SoundEvent SPOTTED;
    public static SoundEvent DISAPPEAR;

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        CAVE_NOISE_1 = registerSound(event, "cavenoise_1");
        CAVE_NOISE_2 = registerSound(event, "cavenoise_2");
        CAVE_NOISE_3 = registerSound(event, "cavenoise_3");
        CAVE_NOISE_4 = registerSound(event, "cavenoise_4");
        CHASE_1 = registerSound(event, "chase_1");
        CHASE_2 = registerSound(event, "chase_2");
        CHASE_3 = registerSound(event, "chase_3");
        CHASE_4 = registerSound(event, "chase_4");
        FLEE_1 = registerSound(event, "flee_1");
        FLEE_2 = registerSound(event, "flee_2");
        HURT_1 = registerSound(event, "dweller_hurt_1");
        HURT_2 = registerSound(event, "dweller_hurt_2");
        HURT_3 = registerSound(event, "dweller_hurt_3");
        HURT_4 = registerSound(event, "dweller_hurt_4");
        DEATH = registerSound(event, "death");
        STEPS_1 = registerSound(event, "chase_steps_1");
        STEPS_2 = registerSound(event, "chase_steps_2");
        STEPS_3 = registerSound(event, "chase_steps_3");
        STEPS_4 = registerSound(event, "chase_steps_4");
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
