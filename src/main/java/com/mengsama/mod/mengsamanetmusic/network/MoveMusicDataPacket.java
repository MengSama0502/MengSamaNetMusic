package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MoveMusicDataPacket(int fromIndex, int toIndex) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(fromIndex);
        buf.writeInt(toIndex);
    }

    public static MoveMusicDataPacket decode(FriendlyByteBuf buf) {
        return new MoveMusicDataPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(MoveMusicDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            return;
        }
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getMainHandItem();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}