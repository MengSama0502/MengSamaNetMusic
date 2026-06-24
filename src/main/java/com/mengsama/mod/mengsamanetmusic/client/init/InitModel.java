package com.mengsama.mod.mengsamanetmusic.client.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.client.renderer.MusicPlayerRenderer;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MengSamaNetMusic.MOD_ID)
public class InitModel {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MUSIC_PLAYER.get(), MusicPlayerRenderer::new);
    }
}
