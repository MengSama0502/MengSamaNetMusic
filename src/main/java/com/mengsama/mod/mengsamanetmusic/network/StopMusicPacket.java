package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.NetworkEvent;
import net.fabricmc.fabric.PacketDistributor;

import java.util.function.Supplier;

public record StopMusicPacket(int playerID, String url) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerID);
        buf.writeUtf(url);
    }

    public static StopMusicPacket decode(FriendlyByteBuf buf) {
        return new StopMusicPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(StopMusicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isServer()) {
            c.enqueueWork(() -> {

                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new StopMusicPacketClient(packet.playerID(), packet.url()));
            });
        }
        c.setPacketHandled(true);
    }
}
