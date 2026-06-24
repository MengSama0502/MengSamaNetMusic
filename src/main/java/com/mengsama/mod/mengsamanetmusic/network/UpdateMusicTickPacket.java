package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.NetworkEvent;

import java.util.function.Supplier;

public record UpdateMusicTickPacket(int slot, int tick) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(tick);
    }

    public static UpdateMusicTickPacket decode(FriendlyByteBuf buf) {
        return new UpdateMusicTickPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(UpdateMusicTickPacket packet, Supplier<NetworkEvent.Context> ctx) {
        var c = ctx.get();
        if (!c.getDirection().getReceptionSide().isServer()) {
            c.setPacketHandled(true);
            return;
        }
        var player = c.getSender();
        if (player == null) {
            c.setPacketHandled(true);
            return;
        }
        c.enqueueWork(() -> {
            int slot = packet.slot();
            if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
                return;
            }
            var stack = player.getInventory().getItem(slot);
            stack.getOrCreateTag().putInt("tick", packet.tick());
        });
        c.setPacketHandled(true);
    }
}
