package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.block.IMusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.block.PortableMusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.init.ModMenuTypes;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.net.fabricmc.fabric;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerPlaylistMenu extends AbstractContainerMenu {
    public static final MenuType<MusicPlayerPlaylistMenu> TYPE = net.fabricmc.fabric.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        var level = inv.player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMusicPlayerBlockEntity musicPlayer) {
            return new MusicPlayerPlaylistMenu(windowId, inv, musicPlayer);
        }
        return new MusicPlayerPlaylistMenu(windowId, inv, (IMusicPlayerBlockEntity) null);
    });

    public static final int BUTTON_PLAY = 0;
    public static final int BUTTON_STOP = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_MODE = 3;
    public static final int BUTTON_PREV = 4;
    public static final int BUTTON_SELECT_BASE = 100;
    public static final int BUTTON_DELETE_BASE = 200;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;

    public static final int INV_X = 14;
    public static final int INV_Y = 342;
    public static final int HOTBAR_Y = 400;

    private final IMusicPlayerBlockEntity blockEntity;

    public MusicPlayerPlaylistMenu(int windowId, Inventory playerInventory, IMusicPlayerBlockEntity blockEntity) {
        super(ModMenuTypes.MUSIC_PLAYER_PLAYLIST.get(), windowId);
        this.blockEntity = blockEntity;

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
    }

    public IMusicPlayerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getPlayIndex() {
        return blockEntity != null ? blockEntity.getPlayIndex() : 0;
    }

    public PlayMode getPlayMode() {
        return blockEntity != null ? blockEntity.getPlayMode() : PlayMode.SEQUENTIAL;
    }

    public boolean isPlaying() {
        return blockEntity != null && blockEntity.isPlay();
    }

    public List<SongInfo> getPlaylist() {
        List<SongInfo> playlist = new ArrayList<>();
        if (blockEntity == null) return playlist;
        var playerInv = blockEntity.getPlayerInv();
        for (int i = 0; i < playerInv.getSlots(); i++) {
            ItemStack cd = playerInv.getStackInSlot(i);
            if (!cd.isEmpty()) {
                SongInfo info = getSongInfoFromCd(cd);
                if (info != null) {
                    playlist.add(info);
                }
            }
        }
        return playlist;
    }

    public SongInfo getSongInfo(int slotIndex) {
        if (blockEntity == null) return null;
        var playerInv = blockEntity.getPlayerInv();
        if (slotIndex >= 0 && slotIndex < playerInv.getSlots()) {
            ItemStack cd = playerInv.getStackInSlot(slotIndex);
            if (!cd.isEmpty()) {
                return getSongInfoFromCd(cd);
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

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (player.level().isClientSide) return true;
        if (blockEntity == null) return true;

        switch (buttonId) {
            case BUTTON_PLAY -> {
                if (blockEntity.isPlay()) {
                    blockEntity.setPlay(false);
                    blockEntity.markDirty();
                    if (blockEntity.getLevel() != null) {
                        ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                                new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                    }
                } else {
                    ItemStack currentCd = blockEntity.getCurrentCd();
                    if (currentCd.isEmpty()) return true;
                    SongInfo info = getSongInfoFromCd(currentCd);
                    if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                        blockEntity.setPlay(true);
                        blockEntity.setPlayToClient(info);
                    }
                }
            }
            case BUTTON_STOP -> {
                blockEntity.setPlay(false);
                blockEntity.markDirty();
                if (blockEntity.getLevel() != null) {
                    ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                            new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                }
            }
            case BUTTON_NEXT -> {
                blockEntity.setPlay(false);
                blockEntity.markDirty();
                if (blockEntity.getLevel() != null) {
                    ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                            new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                }
                blockEntity.advanceToNext();
                ItemStack currentCd = blockEntity.getCurrentCd();
                if (currentCd.isEmpty()) return true;
                SongInfo info = getSongInfoFromCd(currentCd);
                if (info != null) {
                    blockEntity.setPlay(true);
                    blockEntity.setPlayToClient(info);
                }
            }
            case BUTTON_PREV -> {
                blockEntity.setPlay(false);
                blockEntity.markDirty();
                if (blockEntity.getLevel() != null) {
                    ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                            new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                }
                int currentIndex = blockEntity.getPlayIndex();
                var playerInv = blockEntity.getPlayerInv();
                int prevIndex = currentIndex - 1;
                while (prevIndex >= 0 && playerInv.getStackInSlot(prevIndex).isEmpty()) prevIndex--;
                if (prevIndex < 0) {
                    for (int i = playerInv.getSlots() - 1; i >= 0; i--) {
                        if (!playerInv.getStackInSlot(i).isEmpty()) { prevIndex = i; break; }
                    }
                }
                if (prevIndex >= 0) {
                    blockEntity.setPlayIndex(prevIndex);
                    ItemStack currentCd = blockEntity.getCurrentCd();
                    if (!currentCd.isEmpty()) {
                        SongInfo info = getSongInfoFromCd(currentCd);
                        if (info != null) {
                            blockEntity.setPlay(true);
                            blockEntity.setPlayToClient(info);
                        }
                    }
                }
            }
            case BUTTON_MODE -> {
                PlayMode currentMode = blockEntity.getPlayMode();
                blockEntity.setPlayMode(currentMode.getNext());
            }
            default -> {
                if (buttonId >= BUTTON_DELETE_BASE) {
                    int index = buttonId - BUTTON_DELETE_BASE;
                    var playerInv = blockEntity.getPlayerInv();
                    if (index >= 0 && index < playerInv.getSlots()) {
                        if (index == blockEntity.getPlayIndex() && blockEntity.isPlay()) {
                            blockEntity.setPlay(false);
                            blockEntity.markDirty();
                            if (blockEntity.getLevel() != null) {
                                ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                                        new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                            }
                        }
                        playerInv.setStackInSlot(index, ItemStack.EMPTY);
                        blockEntity.markDirty();
                        int currentPlayIndex = blockEntity.getPlayIndex();
                        if (index < currentPlayIndex) {
                            blockEntity.setPlayIndex(currentPlayIndex - 1);
                        } else if (index == currentPlayIndex) {
                            int nextIndex = -1;
                            for (int i = index; i < playerInv.getSlots(); i++) {
                                if (!playerInv.getStackInSlot(i).isEmpty()) { nextIndex = i; break; }
                            }
                            if (nextIndex < 0) {
                                for (int i = index - 1; i >= 0; i--) {
                                    if (!playerInv.getStackInSlot(i).isEmpty()) { nextIndex = i; break; }
                                }
                            }
                            blockEntity.setPlayIndex(nextIndex >= 0 ? nextIndex : 0);
                        }
                    }
                } else if (buttonId >= BUTTON_SELECT_BASE) {
                    int index = buttonId - BUTTON_SELECT_BASE;
                    var playerInv = blockEntity.getPlayerInv();
                    if (index >= 0 && index < playerInv.getSlots() && !playerInv.getStackInSlot(index).isEmpty()) {
                        blockEntity.setPlay(false);
                        blockEntity.markDirty();
                        if (blockEntity.getLevel() != null) {
                            ModNetwork.sendToNearby(blockEntity.getLevel(), blockEntity.getBlockPos(),
                                    new com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient(-1, ""));
                        }
                        blockEntity.setPlayIndex(index);
                        ItemStack currentCd = blockEntity.getCurrentCd();
                        if (!currentCd.isEmpty()) {
                            SongInfo info = getSongInfoFromCd(currentCd);
                            if (info != null) {
                                blockEntity.setPlay(true);
                                blockEntity.setPlayToClient(info);
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
        if (blockEntity == null) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
