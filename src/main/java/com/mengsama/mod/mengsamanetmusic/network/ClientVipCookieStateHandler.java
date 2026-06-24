package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.QqCredentialManager;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import net.fabricmc.fabric.api.SubscribeEvent;
import net.fabricmc.fabric.common.Mod;
import net.fabricmc.fabric.event.ClientPlayerNetworkEvent;

@Mod.EventBusSubscriber(modid = MengSamaNetMusic.MOD_ID, value = Dist.CLIENT)
public final class ClientVipCookieStateHandler {
    private ClientVipCookieStateHandler() {
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {

        SyncVipCookiePacket.CLIENT_HAS_VIP_COOKIE = false;
        MengSamaNetMusic.LOGGER.debug("Client VIP cookie state reset on disconnect");
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {

        boolean hasLocalCred = QqCredentialManager.hasValidCredential();
        if (hasLocalCred) {
            MengSamaNetMusic.LOGGER.info("Client has valid QQ credential, musicid={}",
                    QqCredentialManager.getMusicId());
        }

    }
}
