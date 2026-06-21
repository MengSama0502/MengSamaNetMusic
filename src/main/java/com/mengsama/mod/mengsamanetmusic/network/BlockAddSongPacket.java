package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.block.IMusicPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlockAddSongPacket {
    private final BlockPos blockPos;
    private final SongInfo songInfo;
    private final boolean playNow;

    public BlockAddSongPacket(BlockPos blockPos, SongInfo songInfo, boolean playNow) {
        this.blockPos = blockPos;
        this.songInfo = songInfo;
        this.playNow = playNow;
    }

    public static void encode(BlockAddSongPacket message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.blockPos);
        CompoundTag tag = new CompoundTag();
        SongInfo.serializeNBT(message.songInfo, tag);
        buf.writeNbt(tag);
        buf.writeBoolean(message.playNow);
    }

    public static BlockAddSongPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        CompoundTag tag = buf.readNbt();
        SongInfo info = SongInfo.deserializeNBT(tag);
        boolean playNow = buf.readBoolean();
        return new BlockAddSongPacket(pos, info, playNow);
    }

    public static void handle(BlockAddSongPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) return;

                var level = sender.level();
                if (level.getBlockEntity(message.blockPos) instanceof IMusicPlayerBlockEntity be) {
                    if (message.playNow && message.songInfo.songUrl != null && !message.songInfo.songUrl.isEmpty()) {
                        be.setPlay(true);
                        be.setPlayToClient(message.songInfo);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}