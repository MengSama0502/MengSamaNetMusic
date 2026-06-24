package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

public record StopMusicPacketServer(int playerID, String url) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerID);
        buf.writeUtf(url);
    }

    public static StopMusicPacketServer decode(FriendlyByteBuf buf) {
        return new StopMusicPacketServer(buf.readInt(), buf.readUtf());
    }

    public static void handle(StopMusicPacketServer packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isServer()) {
            ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new StopMusicPacketClient(packet.playerID(), packet.url()));
        }
        c.setPacketHandled(true);
    }
}
