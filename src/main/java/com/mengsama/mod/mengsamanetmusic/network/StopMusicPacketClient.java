package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicSound;
import com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import net.fabricmc.fabric.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record StopMusicPacketClient(int playerID, String url) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerID);
        buf.writeUtf(url);
    }

    public static StopMusicPacketClient decode(FriendlyByteBuf buf) {
        return new StopMusicPacketClient(buf.readInt(), buf.readUtf());
    }

    public static void handle(StopMusicPacketClient packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isClient()) {
            c.enqueueWork(() -> handleClient(packet));
        }
        c.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(StopMusicPacketClient packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<TickableSoundInstance> sounds;
        try {
            sounds = new ArrayList<>(NetMusicListUtil.getTickableSounds());
        } catch (Exception e) {
            return;
        }
        if (packet.playerID == -1) {
            for (TickableSoundInstance sound : sounds) {
                if (sound instanceof NetMusicSound netMusicSound) {
                    netMusicSound.stopSound();
                    var pos = netMusicSound.getPos();
                    var level = mc.level;
                    if (level != null && level.getBlockEntity(pos) instanceof com.mengsama.mod.mengsamanetmusic.block.IMusicPlayerBlockEntity be) {
                        be.setPlay(false);
                    }
                } else if (sound instanceof PlayerNetMusicSound playerSound) {
                    playerSound.stopMusic();
                }
            }
            com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud.clearInfo();
            return;
        }
        Player player = (Player) mc.level.getEntity(packet.playerID);
        if (player == null) return;
        for (TickableSoundInstance sound : sounds) {
            if (sound instanceof PlayerNetMusicSound playerSound) {
                try {
                    if (playerSound.getPlayer() != null
                            && playerSound.getPlayer().getUUID().equals(player.getUUID())) {
                        playerSound.stopMusic();
                    }
                } catch (Exception ignored) {}
            } else if (sound instanceof NetMusicSound netMusicSound) {
                netMusicSound.stopSound();
            }
        }

        com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud.clearInfo();
    }
}
