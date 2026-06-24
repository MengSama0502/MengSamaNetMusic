package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.NetworkEvent;

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
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = MusicPlayerItem.findMusicPlayerItem(player);
                if (!stack.isEmpty()) {
                    MusicPlayerItem.setPlayIndex(stack, packet.index);
                    MusicPlayerItem.setPlayMode(stack, PlayMode.getMode(packet.playModeOrdinal));
                }
            }
            ctx.get().setPacketHandled(true);
        });
    }
}
