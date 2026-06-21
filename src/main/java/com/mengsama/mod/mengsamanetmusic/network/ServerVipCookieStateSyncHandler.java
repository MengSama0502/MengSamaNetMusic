package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.VipCookieState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = MengSamaNetMusic.MOD_ID)
public final class ServerVipCookieStateSyncHandler {
    private ServerVipCookieStateSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToPlayer(serverPlayer);
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        boolean hasCookie = VipCookieState.hasServerVipCookieAvailable();
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncVipCookiePacket(hasCookie));
        MengSamaNetMusic.LOGGER.debug("Synced VIP cookie state to {} (hasCookie={})",
                player.getName().getString(), hasCookie);
    }

    public static void syncToAll() {
        boolean hasCookie = VipCookieState.hasServerVipCookieAvailable();
        SyncVipCookiePacket packet = new SyncVipCookiePacket(hasCookie);
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        MengSamaNetMusic.LOGGER.info("Synced VIP cookie state to all players (hasCookie={})", hasCookie);
    }
}
