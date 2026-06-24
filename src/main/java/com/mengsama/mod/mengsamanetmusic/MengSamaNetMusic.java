package com.mengsama.mod.mengsamanetmusic;

import com.mengsama.mod.mengsamanetmusic.api.NetEaseApi;
import com.mengsama.mod.mengsamanetmusic.api.QqCredentialManager;
import com.mengsama.mod.mengsamanetmusic.config.ConfigManager;
import com.mengsama.mod.mengsamanetmusic.config.ModConfig;
import com.mengsama.mod.mengsamanetmusic.init.*;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import net.fabricmc.fabric.ModLoadingContext;
import net.fabricmc.fabric.common.Mod;
import net.fabricmc.fabric.config.ModConfig.Type;
import net.fabricmc.fabric.event.lifecycle.FMLCommonSetupEvent;
import net.fabricmc.fabric.javafmlmod.FMLJavaModLoadingContext;
import net.fabricmc.fabric.loading.FMLPaths;
import net.fabricmc.fabric.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MengSamaNetMusic.MOD_ID)
public class MengSamaNetMusic {
    public static final String MOD_ID = "mengsamanetmusic";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static NetEaseApi NET_EASE_API;
    public static QqCredentialManager QQ_CREDENTIAL_MANAGER;

    public MengSamaNetMusic() {
        NET_EASE_API = new NetEaseApi();
        QqCredentialManager.init(FMLPaths.CONFIGDIR.get());

        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.CREATIVE_TABS.register(bus);
        ModSounds.SOUND_EVENTS.register(bus);
        ModMenuTypes.MENU_TYPES.register(bus);

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.init());
        ModNetwork.init();

        bus.addListener(this::onCommonSetup);

        net.fabricmc.fabric.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {

    }

    private void onServerStarting(ServerStartingEvent event) {

        ConfigManager.initCookies();
    }
}
