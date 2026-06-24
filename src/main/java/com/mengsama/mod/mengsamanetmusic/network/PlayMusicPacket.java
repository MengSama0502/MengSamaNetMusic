package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import net.fabricmc.fabric.NetworkEvent;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlayMusicPacket {
    private final BlockPos pos;
    private final String url;
    private final String rawUrl;
    private final int timeSecond;
    private final String songName;
    private final long songId;

    public PlayMusicPacket(BlockPos pos, String url, String rawUrl, int timeSecond, String songName, long songId) {
        this.pos = pos;
        this.url = url;
        this.rawUrl = rawUrl;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
    }

    public static PlayMusicPacket decode(FriendlyByteBuf buf) {
        return new PlayMusicPacket(BlockPos.of(buf.readLong()), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readLong());
    }

    public static void encode(PlayMusicPacket message, FriendlyByteBuf buf) {
        buf.writeLong(message.pos.asLong());
        buf.writeUtf(message.url);
        buf.writeUtf(message.rawUrl);
        buf.writeInt(message.timeSecond);
        buf.writeUtf(message.songName);
        buf.writeLong(message.songId);
    }

    public static void handle(PlayMusicPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(PlayMusicPacket message) {
        try {
            URL songUrl = new URI(message.url).toURL();
            Minecraft.getInstance().submitAsync(() -> {
                try {

                    stopExistingBlockMusic(message.pos);

                    net.minecraft.world.level.Level level = Minecraft.getInstance().level;
                    if (level != null && level.getBlockEntity(message.pos) instanceof com.mengsama.mod.mengsamanetmusic.block.IMusicPlayerBlockEntity be) {
                        be.setPlay(true);
                    }
                    com.mengsama.mod.mengsamanetmusic.util.NetMusicSound sound =
                        new com.mengsama.mod.mengsamanetmusic.util.NetMusicSound(message.pos, songUrl, message.timeSecond);
                    Minecraft.getInstance().getSoundManager().play(sound);

                    Minecraft.getInstance().gui.setNowPlaying(Component.literal(message.songName));

                    com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud.setInfoFromPacket(message.songName, message.timeSecond, message.rawUrl, message.songId);
                } catch (Exception e) {
                    com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.error("PlayMusicPacket onHandle error: {}", e.getMessage());
                }
            });
        } catch (MalformedURLException | URISyntaxException ignored) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void stopExistingBlockMusic(@SuppressWarnings("unused") BlockPos pos) {
        try {
            var sounds = new java.util.ArrayList<>(com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil.getTickableSounds());
            for (var sound : sounds) {
                if (sound instanceof com.mengsama.mod.mengsamanetmusic.util.NetMusicSound netSound) {
                    netSound.stopSound();
                } else if (sound instanceof com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound playerSound) {
                    playerSound.stopMusic();
                }
            }
        } catch (Exception ignored) {}
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getUrl() {
        return url;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public int getTimeSecond() {
        return timeSecond;
    }

    public String getSongName() {
        return songName;
    }
}
