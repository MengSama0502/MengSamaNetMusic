package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import net.minecraft.core.NonNullList;
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
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = MusicPlayerItem.findMusicPlayerItem(player);
                if (!stack.isEmpty()) {
                    NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(stack);
                    if (packet.fromIndex >= 0 && packet.fromIndex < cds.size()
                            && packet.toIndex >= 0 && packet.toIndex < cds.size()) {
                        ItemStack temp = cds.get(packet.fromIndex);
                        cds.set(packet.fromIndex, cds.get(packet.toIndex));
                        cds.set(packet.toIndex, temp);
                        MusicPlayerItem.saveAllCdsToItem(stack, cds);
                    }
                }
            }
            ctx.get().setPacketHandled(true);
        });
    }
}
