package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModMenuTypes;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerMenu extends AbstractContainerMenu {
    public static final MenuType<MusicPlayerMenu> TYPE = net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((windowId, inv, data) -> new MusicPlayerMenu(windowId, inv));

    public static final int BUTTON_PLAY = 0;
    public static final int BUTTON_STOP = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_PREV = 3;
    public static final int BUTTON_MODE = 4;
    public static final int BUTTON_SELECT_BASE = 100;
    public static final int BUTTON_DELETE_BASE = 200;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;

    public static final int INV_X = 14;
    public static final int INV_Y = 342;
    public static final int HOTBAR_Y = 400;

    public MusicPlayerMenu(int windowId, Inventory playerInventory) {
        super(ModMenuTypes.MUSIC_PLAYER.get(), windowId);

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
    }

    public int getPlayIndex() {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = MusicPlayerItem.findMusicPlayerItem(player);
            if (!item.isEmpty()) {
                return MusicPlayerItem.getPlayIndex(item);
            }
        }
        return 0;
    }

    public PlayMode getPlayMode() {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = MusicPlayerItem.findMusicPlayerItem(player);
            if (!item.isEmpty()) {
                return MusicPlayerItem.getPlayMode(item);
            }
        }
        return PlayMode.SEQUENTIAL;
    }

    public boolean isPlaying() {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = MusicPlayerItem.findMusicPlayerItem(player);
            if (!item.isEmpty()) {
                return MusicPlayerItem.isPlay(item);
            }
        }
        return false;
    }

    public List<SongInfo> getPlaylist() {
        List<SongInfo> playlist = new ArrayList<>();
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = MusicPlayerItem.findMusicPlayerItem(player);
            if (!item.isEmpty()) {
                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(item);
                for (int i = 0; i < cds.size(); i++) {
                    ItemStack cd = cds.get(i);
                    if (!cd.isEmpty()) {
                        SongInfo info = getSongInfoFromCd(cd);
                        if (info != null) {
                            playlist.add(info);
                        }
                    }
                }
            }
        }
        return playlist;
    }

    public SongInfo getSongInfo(int index) {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = MusicPlayerItem.findMusicPlayerItem(player);
            if (!item.isEmpty()) {
                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(item);
                if (index >= 0 && index < cds.size()) {
                    ItemStack cd = cds.get(index);
                    if (!cd.isEmpty()) {
                        return getSongInfoFromCd(cd);
                    }
                }
            }
        }
        return null;
    }

    private SongInfo getSongInfoFromCd(ItemStack cd) {
        if (cd.getItem() instanceof MusicListItem) {
            return MusicListItem.getSongInfo(cd);
        } else if (cd.getItem() instanceof MusicCDItem) {
            return MusicCDItem.getSongInfo(cd);
        }
        return null;
    }

    private Player getPlayer() {
        for (Slot slot : this.slots) {
            if (slot.container instanceof Inventory inv) {
                return inv.player;
            }
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (player.level().isClientSide) return true;
        if (!(player instanceof ServerPlayer sp)) return true;

        ItemStack playerItem = MusicPlayerItem.findMusicPlayerItem(sp);
        if (playerItem.isEmpty()) return true;

        switch (buttonId) {
            case BUTTON_PLAY -> {
                if (MusicPlayerItem.isPlay(playerItem)) {
                    MusicPlayerItem.setPlay(playerItem, false);
                    ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                            new StopMusicPacketClient(-1, ""));
                } else {
                    ItemStack currentCd = MusicPlayerItem.getCurrentCd(playerItem);
                    if (currentCd.isEmpty()) return true;
                    SongInfo info = getSongInfoFromCd(currentCd);
                    if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                        MusicPlayerItem.setPlayToClient(playerItem, info, sp);
                    }
                }
            }
            case BUTTON_STOP -> {
                MusicPlayerItem.setPlay(playerItem, false);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new StopMusicPacketClient(-1, ""));
            }
            case BUTTON_NEXT -> {
                MusicPlayerItem.setPlay(playerItem, false);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new StopMusicPacketClient(-1, ""));
                MusicPlayerItem.advanceToNext(playerItem);
                ItemStack currentCd = MusicPlayerItem.getCurrentCd(playerItem);
                if (currentCd.isEmpty()) return true;
                SongInfo info = getSongInfoFromCd(currentCd);
                if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                    MusicPlayerItem.setPlayToClient(playerItem, info, sp);
                }
            }
            case BUTTON_PREV -> {
                MusicPlayerItem.setPlay(playerItem, false);
                ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                        new StopMusicPacketClient(-1, ""));
                int currentIndex = MusicPlayerItem.getPlayIndex(playerItem);
                NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                int prevIndex = currentIndex - 1;
                while (prevIndex >= 0 && cds.get(prevIndex).isEmpty()) prevIndex--;
                if (prevIndex < 0) {
                    for (int i = cds.size() - 1; i >= 0; i--) {
                        if (!cds.get(i).isEmpty()) { prevIndex = i; break; }
                    }
                }
                if (prevIndex >= 0) {
                    MusicPlayerItem.setPlayIndex(playerItem, prevIndex);
                    ItemStack cd = MusicPlayerItem.getCurrentCd(playerItem);
                    if (!cd.isEmpty()) {
                        SongInfo info = getSongInfoFromCd(cd);
                        if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                            MusicPlayerItem.setPlayToClient(playerItem, info, sp);
                        }
                    }
                }
            }
            case BUTTON_MODE -> {
                PlayMode currentMode = MusicPlayerItem.getPlayMode(playerItem);
                MusicPlayerItem.setPlayMode(playerItem, currentMode.getNext());
            }
            default -> {
                if (buttonId >= BUTTON_DELETE_BASE) {
                    int index = buttonId - BUTTON_DELETE_BASE;
                    NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                    if (index >= 0 && index < cds.size()) {
                        MusicPlayerItem.setPlay(playerItem, false);
                        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                new StopMusicPacketClient(-1, ""));
                        cds.set(index, ItemStack.EMPTY);
                        MusicPlayerItem.saveAllCdsToItem(playerItem, cds);
                        int currentPlayIndex = MusicPlayerItem.getPlayIndex(playerItem);
                        if (index < currentPlayIndex) {
                            MusicPlayerItem.setPlayIndex(playerItem, currentPlayIndex - 1);
                        } else if (index == currentPlayIndex) {
                            int nextIndex = -1;
                            for (int i = index; i < cds.size(); i++) {
                                if (!cds.get(i).isEmpty()) { nextIndex = i; break; }
                            }
                            if (nextIndex < 0) {
                                for (int i = index - 1; i >= 0; i--) {
                                    if (!cds.get(i).isEmpty()) { nextIndex = i; break; }
                                }
                            }
                            MusicPlayerItem.setPlayIndex(playerItem, nextIndex >= 0 ? nextIndex : 0);
                        }
                    }
                } else if (buttonId >= BUTTON_SELECT_BASE) {
                    int index = buttonId - BUTTON_SELECT_BASE;
                    NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                    if (index >= 0 && index < cds.size() && !cds.get(index).isEmpty()) {
                        MusicPlayerItem.setPlay(playerItem, false);
                        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                                new StopMusicPacketClient(-1, ""));
                        MusicPlayerItem.setPlayIndex(playerItem, index);
                        ItemStack cd = MusicPlayerItem.getCurrentCd(playerItem);
                        if (!cd.isEmpty()) {
                            SongInfo info = getSongInfoFromCd(cd);
                            if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                                MusicPlayerItem.setPlayToClient(playerItem, info, sp);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        if (index >= PLAYER_INV_START && index < PLAYER_INV_START + 9) {
            if (!this.moveItemStackTo(original, PLAYER_INV_START + 9, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START + 9 && index < PLAYER_INV_END) {
            if (!this.moveItemStackTo(original, PLAYER_INV_START, PLAYER_INV_START + 9, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, original);
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !MusicPlayerItem.findMusicPlayerItem(player).isEmpty();
    }
}
