package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.compat.EntityMusicDevice;
import com.mengsama.mod.mengsamanetmusic.init.ModMenuTypes;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.StopMusicPacketClient;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MusicPlayerMenu extends AbstractContainerMenu {
    public static final MenuType<MusicPlayerMenu> TYPE = IForgeMenuType.create(MusicPlayerMenu::fromNetwork);

    public enum Context { PLAYER_HAND, MAID }

    private static MusicPlayerMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
        if (data == null) return forPlayerHand(windowId, inv, null);
        int contextId = data.readUnsignedByte();
        if (contextId == Context.MAID.ordinal()) {
            return forMaid(windowId, inv, data.readUUID(), data.readInt(), data.readUUID());
        }
        return forPlayerHand(windowId, inv, data.readUUID());
    }

    public static final int BUTTON_PLAY = 0;
    public static final int BUTTON_STOP = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_PREV = 3;
    public static final int BUTTON_MODE = 4;
    public static final int BUTTON_BROADCAST = 5;
    public static final int BUTTON_SELECT_BASE = 100;
    public static final int BUTTON_DELETE_BASE = 200;

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;

    private final Player owner;
    private final Context context;
    private ItemStack device;
    private final UUID boundEntityId;
    private final int boundRuntimeEntityId;
    private final UUID boundInstanceId;

    public static final int INV_X = 14;
    public static final int INV_Y = 342;
    public static final int HOTBAR_Y = 400;

    public static MusicPlayerMenu forPlayerHand(int windowId, Inventory playerInventory, UUID instanceId) {
        return new MusicPlayerMenu(windowId, playerInventory, Context.PLAYER_HAND, null, -1, instanceId);
    }

    public static MusicPlayerMenu forMaid(int windowId, Inventory playerInventory, UUID maidId,
                                           int runtimeEntityId, UUID instanceId) {
        return new MusicPlayerMenu(windowId, playerInventory, Context.MAID, maidId, runtimeEntityId, instanceId);
    }

    private MusicPlayerMenu(int windowId, Inventory playerInventory, Context context, UUID boundEntityId,
                            int boundRuntimeEntityId, UUID boundInstanceId) {
        super(ModMenuTypes.MUSIC_PLAYER.get(), windowId);
        this.owner = playerInventory.player;
        this.context = context;
        this.boundEntityId = boundEntityId;
        this.boundRuntimeEntityId = boundRuntimeEntityId;
        this.boundInstanceId = boundInstanceId;
        this.device = context == Context.PLAYER_HAND ? resolveHeldInstance(owner, boundInstanceId)
                : EntityMusicDevice.resolve(owner, boundEntityId, boundRuntimeEntityId, boundInstanceId);

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
            ItemStack item = device;
            if (!item.isEmpty()) {
                return MusicPlayerItem.getPlayIndex(item);
            }
        }
        return 0;
    }

    public PlayMode getPlayMode() {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = device;
            if (!item.isEmpty()) {
                return MusicPlayerItem.getPlayMode(item);
            }
        }
        return PlayMode.SEQUENTIAL;
    }

    public boolean isPlaying() {
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = device;
            if (!item.isEmpty()) {
                return MusicPlayerItem.isPlay(item);
            }
        }
        return false;
    }

    public boolean isBroadcast() {
        return !device.isEmpty() && MusicPlayerItem.isBroadcast(device);
    }

    public boolean isPlayerHandContext() {
        return context == Context.PLAYER_HAND;
    }

    public List<SongInfo> getPlaylist() {
        List<SongInfo> playlist = new ArrayList<>();
        Player player = getPlayer();
        if (player != null) {
            ItemStack item = device;
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
            ItemStack item = device;
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
        return cd.getItem() instanceof MusicListItem ? MusicListItem.getSongInfo(cd) : null;
    }

    public String getTargetId() {
        if (device.isEmpty()) return "missing-item";
        if (boundEntityId != null && owner.level() != null) {
            var living = owner.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    owner.getBoundingBox().inflate(128.0), e -> e.getUUID().equals(boundEntityId)).stream().findFirst().orElse(null);
            if (living != null) return EntityMusicDevice.targetId(living, device);
        }
        return MusicPlayerItem.targetId(owner, device);
    }

    private void sendStop(ServerPlayer player) {
        String targetId = getTargetId();
        if (boundEntityId == null) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StopMusicPacketClient(targetId));
        } else {
            var entity = player.serverLevel().getEntity(boundRuntimeEntityId);
            if (entity != null) {
                ModNetwork.sendToNearby(player.serverLevel(), entity.blockPosition(), new StopMusicPacketClient(targetId));
                if (entity instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid)
                    com.mengsama.mod.mengsamanetmusic.compat.MaidLyricSynchronizer.stop(maid.getUUID());
            }
        }
    }

    private void sendPause(ServerPlayer player, boolean paused) {
        String targetId = getTargetId();
        long generation = com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.currentRequestGeneration(
                boundEntityId == null ? player : player.serverLevel().getEntity(boundRuntimeEntityId));
        var packet = new com.mengsama.mod.mengsamanetmusic.network.PauseMusicPacketClient(targetId, paused, generation);
        if (boundEntityId == null) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } else {
            var entity = player.serverLevel().getEntity(boundRuntimeEntityId);
            if (entity != null) ModNetwork.sendToNearby(player.serverLevel(), entity.blockPosition(), packet);
        }
    }

    public void playSelected(ItemStack stack, SongInfo info, ServerPlayer player) {
        playToClient(stack, info, player);
    }

    private void playToClient(ItemStack stack, SongInfo info, ServerPlayer player) {
        if (boundEntityId == null) {
            MusicPlayerItem.setPlayToClient(stack, info, player);
        } else {
            var entity = player.serverLevel().getEntity(boundEntityId);
            if (entity instanceof net.minecraft.world.entity.LivingEntity living)
                MusicPlayerItem.setPlayToEntity(stack, info, living);
        }
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

        ItemStack playerItem = resolveValidatedDevice(player);
        if (playerItem.isEmpty()) {
            com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.LOGGER.warn("Rejected stale music menu action player={} container={}", player.getUUID(), containerId);
            return true;
        }

        switch (buttonId) {
            case BUTTON_PLAY -> {
                if (MusicPlayerItem.isPlay(playerItem)) {
                    boolean paused = !MusicPlayerItem.isPaused(playerItem);
                    // Keep IsPlay true so the maid tracker, lyric session, generation and retained stream survive.
                    MusicPlayerItem.setPaused(playerItem, paused);
                    sendPause(sp, paused);
                } else {
                    ItemStack currentCd = MusicPlayerItem.getCurrentCd(playerItem);
                    if (currentCd.isEmpty()) return true;
                    SongInfo info = getSongInfoFromCd(currentCd);
                    if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                        playToClient(playerItem, info, sp);
                    }
                }
            }
            case BUTTON_STOP -> {
                MusicPlayerItem.setPlay(playerItem, false);
                MusicPlayerItem.setCurrentTime(playerItem, 0);
                sendStop(sp);
            }
            case BUTTON_NEXT -> {
                MusicPlayerItem.setPlay(playerItem, false);
                sendStop(sp);
                MusicPlayerItem.advanceToNext(playerItem);
                ItemStack currentCd = MusicPlayerItem.getCurrentCd(playerItem);
                if (currentCd.isEmpty()) return true;
                SongInfo info = getSongInfoFromCd(currentCd);
                if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                    playToClient(playerItem, info, sp);
                }
            }
            case BUTTON_PREV -> {
                MusicPlayerItem.setPlay(playerItem, false);
                sendStop(sp);
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
                            playToClient(playerItem, info, sp);
                        }
                    }
                }
            }
            case BUTTON_MODE -> {
                PlayMode currentMode = MusicPlayerItem.getPlayMode(playerItem);
                MusicPlayerItem.setPlayMode(playerItem, currentMode.getNext());
            }
            case BUTTON_BROADCAST -> {
                if (context == Context.PLAYER_HAND) {
                    boolean broadcast = !MusicPlayerItem.isBroadcast(playerItem);
                    MusicPlayerItem.setBroadcast(playerItem, broadcast);
                    String targetId = MusicPlayerItem.targetId(sp, playerItem);
                    if (!broadcast) {
                        ModNetwork.sendToNearby(sp.serverLevel(), sp.blockPosition(),
                                new com.mengsama.mod.mengsamanetmusic.network.PlayerHudVisibilityPacket(targetId, false));
                    } else if (MusicPlayerItem.isPlay(playerItem)) {
                        ItemStack currentCd = MusicPlayerItem.getCurrentCd(playerItem);
                        SongInfo info = getSongInfoFromCd(currentCd);
                        if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                            MusicPlayerItem.setPlayToClient(playerItem, info, sp);
                        }
                    }
                }
            }
            default -> {
                if (buttonId >= BUTTON_DELETE_BASE) {
                    int index = buttonId - BUTTON_DELETE_BASE;
                    NonNullList<ItemStack> cds = MusicPlayerItem.loadAllCds(playerItem);
                    if (index >= 0 && index < cds.size()) {
                        MusicPlayerItem.setPlay(playerItem, false);
                        sendStop(sp);
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
                        sendStop(sp);
                        MusicPlayerItem.setPlayIndex(playerItem, index);
                        ItemStack cd = MusicPlayerItem.getCurrentCd(playerItem);
                        if (!cd.isEmpty()) {
                            SongInfo info = getSongInfoFromCd(cd);
                            if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
                                playToClient(playerItem, info, sp);
                            }
                        }
                    }
                }
            }
        }
        syncAuthoritativeState(sp);
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

    public ItemStack getDevice() { return device; }
    public void applyAuthoritativeTag(net.minecraft.nbt.CompoundTag tag) {
        if (!device.isEmpty()) device.setTag(tag.copy());
    }
    public void syncAuthoritativeState(ServerPlayer player) {
        if (!device.isEmpty()) {
            net.minecraft.nbt.CompoundTag tag = device.getTag() == null ? new net.minecraft.nbt.CompoundTag() : device.getTag().copy();
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new com.mengsama.mod.mengsamanetmusic.network.MaidDeviceSyncPacket(containerId, tag));
            broadcastChanges();
        }
    }
    public UUID getBoundEntityId() { return boundEntityId; }
    public int getBoundRuntimeEntityId() { return boundRuntimeEntityId; }
    public UUID getBoundInstanceId() { return boundInstanceId; }

    private static ItemStack resolveHeldInstance(Player player, UUID instanceId) {
        if (instanceId == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof MusicPlayerItem
                && MusicPlayerItem.getOrCreateInstanceId(main).equals(instanceId)) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof MusicPlayerItem
                && MusicPlayerItem.getOrCreateInstanceId(off).equals(instanceId)) return off;
        return ItemStack.EMPTY;
    }

    /** 按入口上下文隔离校验；普通手持入口绝不执行女仆实体或任务验证。 */
    public ItemStack resolveValidatedDevice(Player player) {
        ItemStack resolved = context == Context.PLAYER_HAND
                ? resolveHeldInstance(player, boundInstanceId)
                : EntityMusicDevice.resolve(player, boundEntityId, boundRuntimeEntityId, boundInstanceId);
        if (resolved.isEmpty()) return ItemStack.EMPTY;
        if (context == Context.MAID) {
            var entity = player.level().getEntity(boundRuntimeEntityId);
            if (!(entity instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid maid)
                    || !maid.getUUID().equals(boundEntityId)
                    || !maid.isOwnedBy(player) || !maid.isAlive() || maid.isSleeping()
                    || maid.getTask() == null
                    || !com.mengsama.mod.mengsamanetmusic.compat.TouhouLittleMaidExtension.MUSIC_TASK_UID.equals(maid.getTask().getUid())
                    || player.distanceToSqr(maid) >= 25.0) return ItemStack.EMPTY;
        }
        device = resolved;
        return resolved;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !resolveValidatedDevice(player).isEmpty();
    }
}
