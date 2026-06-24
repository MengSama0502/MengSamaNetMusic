package com.mengsama.mod.mengsamanetmusic.util;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayerPlayMusicPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mengsamanetmusic")
public class EventHandler {

    private static final String ACTIVE_MUSIC_TAG = "ActiveMusic";

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        handlePlayerEvent(event);
    }

    @SubscribeEvent
    public static void onSpawn(PlayerEvent.PlayerRespawnEvent event) {
        handlePlayerEvent(event);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        handlePlayerEvent(event);
    }

    private static void handlePlayerEvent(PlayerEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && hasActiveMusic(player)) {
            resumeMusic(player);
        }
    }

    public static void saveActiveMusic(Player player, String url, int time, String songName, int slot, SongInfo info) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag musicData = new CompoundTag();
        musicData.putString("url", url);
        musicData.putInt("time", time);
        musicData.putString("songName", songName);
        musicData.putInt("slot", slot);
        CompoundTag infoTag = new CompoundTag();
        SongInfo.serializeNBT(info, infoTag);
        musicData.put("info", infoTag);
        persistentData.put(ACTIVE_MUSIC_TAG, musicData);
    }

    public static void clearActiveMusic(Player player) {
        player.getPersistentData().remove(ACTIVE_MUSIC_TAG);
    }

    private static boolean hasActiveMusic(Player player) {
        return player.getPersistentData().contains(ACTIVE_MUSIC_TAG);
    }

    private static void resumeMusic(Player player) {
        CompoundTag musicData = player.getPersistentData().getCompound(ACTIVE_MUSIC_TAG);
        String url = musicData.getString("url");
        int time = musicData.getInt("time");
        String songName = musicData.getString("songName");
        int slot = musicData.getInt("slot");
        SongInfo info = SongInfo.deserializeNBT(musicData.getCompound("info"));

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendToClientPlayer(
                new PlayerPlayMusicPacket(serverPlayer.getId(), url, time, songName, slot, info),
                serverPlayer
            );
        }
    }
}
