package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record StopMusicPacket(String targetId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(targetId);
    }

    public static StopMusicPacket decode(FriendlyByteBuf buf) {
        return new StopMusicPacket(buf.readUtf());
    }

    public static void handle(StopMusicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isServer()) {
            c.enqueueWork(() -> {

                var sender = c.getSender();
                if (sender != null) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                            new StopMusicPacketClient(packet.targetId()));
                }
            });
        }
        c.setPacketHandled(true);
    }
}
