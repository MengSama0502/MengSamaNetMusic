package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DeleteMusicDataPacket(int index) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(index);
    }

    public static DeleteMusicDataPacket decode(FriendlyByteBuf buf) {
        return new DeleteMusicDataPacket(buf.readInt());
    }

    public static void handle(DeleteMusicDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = MusicPlayerItem.findMusicPlayerItem(player);
                if (!stack.isEmpty()) {
                    NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(stack);
                    if (packet.index >= 0 && packet.index < cds.size()) {
                        cds.set(packet.index, ItemStack.EMPTY);
                        MusicPlayerItem.saveAllCdsToItem(stack, cds);
                    }
                }
            }
            ctx.get().setPacketHandled(true);
        });
    }
}
