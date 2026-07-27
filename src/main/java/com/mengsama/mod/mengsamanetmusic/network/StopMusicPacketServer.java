package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record StopMusicPacketServer(String targetId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(targetId);
    }

    public static StopMusicPacketServer decode(FriendlyByteBuf buf) {
        return new StopMusicPacketServer(buf.readUtf());
    }

    public static void handle(StopMusicPacketServer packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (c.getDirection().getReceptionSide().isServer()) {
            var sender = c.getSender();
            if (sender != null) {
                var menu = sender.containerMenu instanceof com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerMenu m ? m : null;
                String expected = menu != null ? menu.getTargetId() : null;
                if (expected != null && expected.equals(packet.targetId())) {
                    com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.setPlay(menu.getDevice(), false);
                    ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                                    sender.getX(), sender.getY(), sender.getZ(), 64.0, sender.level().dimension())),
                            new StopMusicPacketClient(packet.targetId()));
                }
            }
        }
        c.setPacketHandled(true);
    }
}
