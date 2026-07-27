package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModItems;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
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

                com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.info(
                        "[播放阶段] 请求已收 action=add-song player={} playNow={}", sender.getUUID(), message.playNow);
                com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerMenu menu =
                        sender.containerMenu instanceof com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerMenu m ? m : null;
                ItemStack playerItem = menu != null ? menu.resolveValidatedDevice(sender) : ItemStack.EMPTY;
                if (playerItem.isEmpty()) {
                    com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.warn(
                            "[播放阶段] 设备验证失败 player={}", sender.getUUID());
                    sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("音乐请求失败：播放器设备验证失败，请重新打开界面"));
                    return;
                }
                com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.info(
                        "[播放阶段] 设备验证通过 player={} device={}", sender.getUUID(), MusicPlayerItem.getOrCreateInstanceId(playerItem));

                ItemStack cdStack = MusicListItem.addSongInfo(message.songInfo,
                        new ItemStack(ModItems.MUSIC_LIST.get()));
                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                for (ItemStack existingCd : cds) {
                    if (existingCd.isEmpty()) continue;
                    SongInfo existing = MusicListItem.getSongInfo(existingCd);
                    if (message.songInfo.sameIdentity(existing)) {
                        sender.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.mengsamanetmusic.duplicate_song"));
                        return;
                    }
                }
                int targetSlot = -1;
                for (int i = 0; i < cds.size(); i++) {
                    if (cds.get(i).isEmpty()) {
                        targetSlot = i;
                        break;
                    }
                }

                if (targetSlot >= 0) {
                    com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.info(
                            "[播放阶段] 曲目读取成功 player={} identity={} slot={}", sender.getUUID(), message.songInfo.identityKey(), targetSlot);
                    MusicPlayerItem.saveCdToItem(playerItem, targetSlot, cdStack);
                    com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.debug("Added song {} to device {} slot {}",
                            message.songInfo.identityKey(), MusicPlayerItem.getOrCreateInstanceId(playerItem), targetSlot);

                    if (menu != null) menu.syncAuthoritativeState(sender);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
