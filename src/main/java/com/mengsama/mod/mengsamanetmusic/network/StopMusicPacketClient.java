package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.client.audio.ClientMusicPlayback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record StopMusicPacketClient(String targetId) {
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(targetId); }
    public static StopMusicPacketClient decode(FriendlyByteBuf buf) { return new StopMusicPacketClient(buf.readUtf()); }
    public static void handle(StopMusicPacketClient packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isClient()) c.enqueueWork(() -> handleClient(packet));
        c.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(StopMusicPacketClient packet) {
        ClientMusicPlayback.stop(packet.targetId);
        com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud.onDeviceStopped(packet.targetId);
    }
}
