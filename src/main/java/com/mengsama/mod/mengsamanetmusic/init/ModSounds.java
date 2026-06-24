package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENTS, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<SoundEvent> NET_MUSIC = SOUND_EVENTS.register("net_music",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MengSamaNetMusic.MOD_ID, "net_music")));
}
