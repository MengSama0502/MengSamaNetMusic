package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import net.fabricmc.fabric.NetworkEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

public record PlayerPlayMusicPacket(int playerID, String url, int timeSecond, String songName, int slot, SongInfo info) {

    public PlayerPlayMusicPacket(int playerID, String url, int timeSecond, int slot) {
        this(playerID, url, timeSecond, "", slot, new SongInfo(url, "", timeSecond));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerID);
        buf.writeUtf(url);
        buf.writeInt(timeSecond);
        buf.writeUtf(songName);
        buf.writeInt(slot);
        var tag = new CompoundTag();
        SongInfo.serializeNBT(info, tag);
        buf.writeNbt(tag);
    }

    public static PlayerPlayMusicPacket decode(FriendlyByteBuf buf) {
        return new PlayerPlayMusicPacket(
                buf.readInt(), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readInt(),
                SongInfo.deserializeNBT(Objects.requireNonNull(buf.readNbt()))
        );
    }

    public static void handle(PlayerPlayMusicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isClient()) {
            c.enqueueWork(() -> handleClient(packet));
        }
        c.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PlayerPlayMusicPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.submitAsync(() -> {
            try {
                stopExistingPlayerMusic(packet.playerID);
                URL songUrl = new URL(packet.url);
                Player player = (Player) mc.level.getEntity(packet.playerID);
                if (player != null) {
                    ItemStack playerItem = MusicPlayerItem.findMusicPlayerItem(player);
                    if (!playerItem.isEmpty()) {
                        MusicPlayerItem.setPlay(playerItem, true);
                    }
                    PlayerNetMusicSound sound = new PlayerNetMusicSound(player, songUrl, packet.timeSecond, packet.slot);
                    mc.getSoundManager().play(sound);
                    mc.gui.setNowPlaying(Component.literal(packet.songName));
                    MusicInfoHud.setInfoFromPacket(packet.info);
                }
            } catch (MalformedURLException ignored) {
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("PlayerPlayMusicPacket handleClient error: {}", e.getMessage());
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static void stopExistingPlayerMusic(int playerID) {
        try {
            var sounds = new java.util.ArrayList<>(NetMusicListUtil.getTickableSounds());
            for (var sound : sounds) {
                if (sound instanceof PlayerNetMusicSound playerSound) {
                    if (playerSound.getPlayer() != null
                            && playerSound.getPlayer().getId() == playerID) {
                        try {
                            playerSound.stopMusic();
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
