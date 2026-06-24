package com.mengsama.mod.mengsamanetmusic.client.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.client.renderer.MusicPlayerRenderer;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import net.fabricmc.api.Dist;
import net.fabricmc.fabric.event.EntityRenderersEvent;
import net.fabricmc.fabric.api.SubscribeEvent;
import net.fabricmc.fabric.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MengSamaNetMusic.MOD_ID)
public class InitModel {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MUSIC_PLAYER.get(), MusicPlayerRenderer::new);
    }
}
