package com.mengsama.mod.mengsamanetmusic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MusicListDataPacket(int index, int playModeOrdinal) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeInt(playModeOrdinal);
    }

    public static MusicListDataPacket decode(FriendlyByteBuf buf) {
        return new MusicListDataPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(MusicListDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
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