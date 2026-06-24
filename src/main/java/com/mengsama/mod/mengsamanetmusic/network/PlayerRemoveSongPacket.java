package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerRemoveSongPacket {
    private final int slotIndex;

    public PlayerRemoveSongPacket(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public static void encode(PlayerRemoveSongPacket message, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeInt(message.slotIndex);
    }

    public static PlayerRemoveSongPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new PlayerRemoveSongPacket(buf.readInt());
    }

    public static void handle(PlayerRemoveSongPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) return;

                ItemStack playerItem = MusicPlayerItem.findMusicPlayerItem(sender);
                if (playerItem.isEmpty()) return;

                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                if (message.slotIndex >= 0 && message.slotIndex < cds.size()) {
                    boolean wasCurrentSong = (message.slotIndex == MusicPlayerItem.getPlayIndex(playerItem));

                    cds.set(message.slotIndex, ItemStack.EMPTY);
                    MusicPlayerItem.saveAllCdsToItem(playerItem, cds);

                    int currentPlayIndex = MusicPlayerItem.getPlayIndex(playerItem);
                    if (message.slotIndex < currentPlayIndex) {
                        MusicPlayerItem.setPlayIndex(playerItem, currentPlayIndex - 1);
                    } else if (wasCurrentSong) {

                        ModNetwork.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                                new StopMusicPacketClient(sender.getId(), ""));
                    }

                    try {
                        sender.getInventory().setChanged();
                        if (sender.containerMenu != null) {
                            sender.containerMenu.broadcastChanges();
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
        context.setPacketHandled(true);
    }
}
