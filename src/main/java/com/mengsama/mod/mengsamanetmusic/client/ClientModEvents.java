package com.mengsama.mod.mengsamanetmusic.client;

import com.mengsama.mod.mengsamanetmusic.config.ConfigManager;
import com.mengsama.mod.mengsamanetmusic.gui.*;
import com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud;
import com.mengsama.mod.mengsamanetmusic.hud.MusicListLayer;
import com.mengsama.mod.mengsamanetmusic.init.ModMenuTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import net.fabricmc.fabric.event.RenderGuiEvent;
import net.fabricmc.fabric.api.SubscribeEvent;
import net.fabricmc.fabric.event.lifecycle.FMLClientSetupEvent;
import net.fabricmc.fabric.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        ConfigManager.initCookies();

        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    ModMenuTypes.MUSIC_PLAYER.get(),
                    MusicPlayerScreen::new);

            net.minecraft.client.gui.screens.MenuScreens.register(
                    ModMenuTypes.MUSIC_PLAYER_PLAYLIST.get(),
                    MusicPlayerPlaylistScreen::new);
        });
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            MusicInfoHud.render(guiGraphics);
            MusicListLayer.render(guiGraphics);
        }
    }
}
