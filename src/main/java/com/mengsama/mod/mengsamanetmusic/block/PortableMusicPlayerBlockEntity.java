package com.mengsama.mod.mengsamanetmusic.block;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayMusicPacket;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PortableMusicPlayerBlockEntity extends BlockEntity implements IMusicPlayerBlockEntity {

    private static final String CD_ITEMS_TAG = "ItemStacksCD";
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String IS_PAUSED_TAG = "IsPaused";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private static final String PLAY_INDEX_TAG = "PlayIndex";
    private static final String PLAY_MODE_TAG = "PlayMode";
    private static final int SLOT_COUNT = 54;

    private final ItemStackHandler playerInv = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof MusicListItem;
        }

        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    private LazyOptional<IItemHandler> playerInvHandler;
    private boolean isPlay = false;
    private boolean isPaused = false;
    private int currentTime;
    private boolean hasSignal = false;
    private int playIndex = 0;
    private PlayMode playMode = PlayMode.SEQUENTIAL;
    private boolean autoAdvanceArmed;
    private final java.util.concurrent.atomic.AtomicLong playRequestGeneration = new java.util.concurrent.atomic.AtomicLong();

    public PortableMusicPlayerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.PORTABLE_MUSIC_PLAYER.get(), blockPos, blockState);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        compound.put(CD_ITEMS_TAG, playerInv.serializeNBT());
        compound.putBoolean(IS_PLAY_TAG, isPlay);
        compound.putBoolean(IS_PAUSED_TAG, isPaused);
        compound.putInt(CURRENT_TIME_TAG, currentTime);
        compound.putBoolean(SIGNAL_TAG, hasSignal);
        compound.putInt(PLAY_INDEX_TAG, playIndex);
        compound.putInt(PLAY_MODE_TAG, playMode.ordinal());
        super.saveAdditional(compound);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        playerInv.deserializeNBT(nbt.getCompound(CD_ITEMS_TAG));
        isPlay = nbt.getBoolean(IS_PLAY_TAG);
        isPaused = nbt.getBoolean(IS_PAUSED_TAG);
        currentTime = nbt.getInt(CURRENT_TIME_TAG);
        autoAdvanceArmed = currentTime > 0;
        hasSignal = nbt.getBoolean(SIGNAL_TAG);
        playIndex = nbt.contains(PLAY_INDEX_TAG) ? nbt.getInt(PLAY_INDEX_TAG) : 0;
        playMode = nbt.contains(PLAY_MODE_TAG) ? PlayMode.getMode(nbt.getInt(PLAY_MODE_TAG)) : PlayMode.SEQUENTIAL;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStackHandler getPlayerInv() {
        return playerInv;
    }

    public int getPlayIndex() {
        return playIndex;
    }

    public void setPlayIndex(int index) {
        this.playIndex = index;
        markDirty();
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public void setPlayMode(PlayMode mode) {
        this.playMode = mode == null ? PlayMode.SEQUENTIAL : mode;
        markDirty();
    }

    public ItemStack getCurrentCd() {
        ItemStack stack = playerInv.getStackInSlot(playIndex);
        if (!stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack slot = playerInv.getStackInSlot(i);
            if (!slot.isEmpty()) {
                playIndex = i;
                return slot;
            }
        }
        return ItemStack.EMPTY;
    }

    public void advanceToNext() {
        int[] counts = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack cd = playerInv.getStackInSlot(i);
            counts[i] = cd.getItem() instanceof MusicListItem ? MusicListItem.getSongCount(cd) : (cd.isEmpty() ? 0 : 1);
        }
        ItemStack current = playIndex >= 0 && playIndex < SLOT_COUNT ? playerInv.getStackInSlot(playIndex) : ItemStack.EMPTY;
        int currentSong = current.getItem() instanceof MusicListItem ? MusicListItem.getSongIndex(current) : 0;
        PlayMode.TrackPosition next = PlayMode.nextTrack(playMode, playIndex, currentSong, counts,
                bound -> RandomSource.create().nextInt(bound));
        playIndex = next.slotIndex();
        ItemStack selected = playerInv.getStackInSlot(playIndex);
        if (selected.getItem() instanceof MusicListItem) MusicListItem.setSongIndex(selected, next.songIndex());
        markDirty();
    }

    private int findFirstNonEmptyIndex() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!playerInv.getStackInSlot(i).isEmpty()) return i;
        }
        return -1;
    }

    private int findLastNonEmptyIndex() {
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            if (!playerInv.getStackInSlot(i).isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            if (this.playerInvHandler == null) {
                this.playerInvHandler = LazyOptional.of(() -> this.playerInv);
            }
            return this.playerInvHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setBlockState(BlockState blockState) {
        super.setBlockState(blockState);
        if (this.playerInvHandler != null) {
            LazyOptional<?> oldHandler = this.playerInvHandler;
            this.playerInvHandler = null;
            oldHandler.invalidate();
        }
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
        if (!play) isPaused = false;
        markDirty();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused && isPlay;
        markDirty();
    }

    public void setPlayToClient(SongInfo info) {
        setPlayToClient(info, 0L, 0);
    }

    public void setPlayToClient(SongInfo info, long refreshNonce) {
        setPlayToClient(info, refreshNonce, 0);
    }

    @Override
    public void setPlayToClient(SongInfo info, int startSecond) {
        setPlayToClient(info, 0L, startSecond, false);
    }

    @Override
    public void seekToClient(SongInfo info, int startSecond, boolean paused) {
        setPlayToClient(info, 0L, startSecond, paused);
    }

    private void setPlayToClient(SongInfo info, long refreshNonce, int startSecond) {
        setPlayToClient(info, refreshNonce, startSecond, false);
    }

    private void setPlayToClient(SongInfo info, long refreshNonce, int startSecond, boolean preservePause) {
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            SongInfo clone = info.clone();
            long requestGeneration = this.playRequestGeneration.incrementAndGet();
            this.isPlay = true;
            this.isPaused = preservePause;
            this.markDirty();
            com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.resolveUrlAsync(clone).thenAcceptAsync(resolved -> {
                try {
                    if (this.isRemoved()) return;
                    if (!this.isPlay || this.playRequestGeneration.get() != requestGeneration) return;
                    ItemStack currentCd = this.getCurrentCd();
                    SongInfo stable = com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.stableSongInfo(resolved, clone);
                    if (currentCd.getItem() instanceof MusicListItem) MusicListItem.setSongInfo(stable, currentCd);
                    this.markDirty();
                    int clampedStart = Math.max(0, Math.min(resolved.songTime, startSecond));
                    this.setCurrentTime(Math.max(1, (resolved.songTime - clampedStart) * 20 + 64));
                    this.markDirty();
                    String url = resolved.songUrl;
                    String targetId = blockTargetId();
                    com.mengsama.mod.mengsamanetmusic.network.PlaybackRefreshSessions.publish(targetId, requestGeneration, stable, targetId);
                    PlayMusicPacket msg = new PlayMusicPacket(
                            worldPosition, targetId, url, stable.rawUrl, resolved.songTime, resolved.songName, requestGeneration, refreshNonce, resolved, clampedStart
                    );
                    ModNetwork.sendToNearby(level, worldPosition, msg);
                    if (preservePause) ModNetwork.sendToNearby(level, worldPosition,
                            new com.mengsama.mod.mengsamanetmusic.network.PauseMusicPacketClient(
                                    targetId, true, requestGeneration));
                } catch (Exception e) {
                    MengSamaNetMusic.LOGGER.error("setPlayToClient error: {}", e.getMessage());
                }
            }, server);
        }
    }

    public long currentRequestGeneration() {
        return playRequestGeneration.get();
    }

    public String blockTargetId() {
        return "block:" + level.dimension().location() + ":" + worldPosition.asLong();
    }

    public void markDirty() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (playerInvHandler != null) {
            playerInvHandler.invalidate();
            playerInvHandler = null;
        }
    }

    public void setCurrentTime(int time) {
        this.currentTime = time;
        this.autoAdvanceArmed = time > 0;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public boolean hasSignal() {
        return hasSignal;
    }

    public void setSignal(boolean signal) {
        this.hasSignal = signal;
    }

    public void tickTime() {
        if (currentTime > 0) {
            currentTime--;
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, 2, 1));
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, PortableMusicPlayerBlockEntity te) {
        if (level.isClientSide || !te.isPlay() || te.isPaused()) return;
        te.tickTime();
        if (te.getCurrentTime() == 0 && te.autoAdvanceArmed) {
            te.autoAdvanceArmed = false;
            te.advanceToNext();
            ItemStack currentCd = te.getCurrentCd();
            if (currentCd.isEmpty()) {
                return;
            }
            SongInfo songInfo = MusicListItem.getSongInfo(currentCd);
            if (songInfo != null) {
                te.setPlayToClient(songInfo);
            }
        }
    }
}
