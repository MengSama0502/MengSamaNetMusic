package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.fabricmc.fabric.DeferredRegister;
import net.fabricmc.fabric.net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENTS, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<SoundEvent> NET_MUSIC = SOUND_EVENTS.register("net_music",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MengSamaNetMusic.MOD_ID, "net_music")));
}
