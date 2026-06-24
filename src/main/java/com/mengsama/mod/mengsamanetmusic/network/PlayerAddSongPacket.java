package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModItems;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerAddSongPacket {
    private final SongInfo songInfo;
    private final boolean playNow;

    public PlayerAddSongPacket(SongInfo songInfo, boolean playNow) {
        this.songInfo = songInfo;
        this.playNow = playNow;
    }

    public static void encode(PlayerAddSongPacket message, FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        SongInfo.serializeNBT(message.songInfo, tag);
        buf.writeNbt(tag);
        buf.writeBoolean(message.playNow);
    }

    public static PlayerAddSongPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        SongInfo info = SongInfo.deserializeNBT(tag);
        boolean playNow = buf.readBoolean();
        return new PlayerAddSongPacket(info, playNow);
    }

    public static void handle(PlayerAddSongPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) return;

                ItemStack playerItem = MusicPlayerItem.findMusicPlayerItem(sender);
                if (playerItem.isEmpty()) return;

                ItemStack cdStack = MusicCDItem.setSongInfo(message.songInfo,
                        new ItemStack(ModItems.MUSIC_CD.get()));
                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                int targetSlot = -1;
                for (int i = 0; i < cds.size(); i++) {
                    if (cds.get(i).isEmpty()) {
                        targetSlot = i;
                        break;
                    }
                }

                if (targetSlot >= 0) {
                    MusicPlayerItem.saveCdToItem(playerItem, targetSlot, cdStack);

                    if (message.playNow && message.songInfo.songUrl != null && !message.songInfo.songUrl.isEmpty()) {
                        MusicPlayerItem.setPlayIndex(playerItem, targetSlot);
                        MusicPlayerItem.setPlayToClient(playerItem, message.songInfo, sender);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
