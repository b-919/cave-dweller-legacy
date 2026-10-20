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

    public static void register() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        CAVE_NOISE = registerSound(event, "cavenoise");
        CHASE_1 = registerSound(event, "chase_1");
        CHASE_2 = registerSound(event, "chase_2");
        CHASE_3 = registerSound(event, "chase_3");
        CHASE_4 = registerSound(event, "chase_4");
        FLEE_1 = registerSound(event, "flee_1");
        FLEE_2 = registerSound(event, "flee_2");
        HURT_1 = registerSound(event, "hurt_1");
        HURT_2 = registerSound(event, "hurt_2");
        HURT_3 = registerSound(event, "hurt_3");
        HURT_4 = registerSound(event, "hurt_4");
        DEATH = registerSound(event, "death");
        STEPS_1 = registerSound(event, "steps_1");
        STEPS_2 = registerSound(event, "steps_2");
        STEPS_3 = registerSound(event, "steps_3");
        STEPS_4 = registerSound(event, "steps_4");
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
