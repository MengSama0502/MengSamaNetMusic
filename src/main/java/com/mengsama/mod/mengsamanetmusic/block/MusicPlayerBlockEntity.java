package com.mengsama.mod.mengsamanetmusic.block;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.init.ModBlockEntities;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayMusicPacket;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
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

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock.CYCLE_DISABLE;

public class MusicPlayerBlockEntity extends BlockEntity implements IMusicPlayerBlockEntity, GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final String CD_ITEMS_TAG = "ItemStacksCD";
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private static final String PLAY_INDEX_TAG = "PlayIndex";
    private static final String PLAY_MODE_TAG = "PlayMode";
    private static final int SLOT_COUNT = 54;

    private final ItemStackHandler playerInv = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof MusicCDItem || stack.getItem() instanceof MusicListItem;
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
    private int currentTime;
    private boolean hasSignal = false;
    private int playIndex = 0;
    private PlayMode playMode = PlayMode.SEQUENTIAL;

    public MusicPlayerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.MUSIC_PLAYER.get(), blockPos, blockState);
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        compound.put(CD_ITEMS_TAG, playerInv.serializeNBT());
        compound.putBoolean(IS_PLAY_TAG, isPlay);
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
        currentTime = nbt.getInt(CURRENT_TIME_TAG);
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
        this.playMode = mode;
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
        int firstIndex = findFirstNonEmptyIndex();
        int lastIndex = findLastNonEmptyIndex();
        if (firstIndex < 0) return;

        switch (playMode) {
            case RANDOM -> {

                int count = 0;
                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (!playerInv.getStackInSlot(i).isEmpty()) count++;
                }
                if (count > 1) {
                    int r = RandomSource.create().nextInt(count);
                    for (int i = 0; i < SLOT_COUNT; i++) {
                        if (!playerInv.getStackInSlot(i).isEmpty()) {
                            if (r == 0) {
                                playIndex = i;
                                break;
                            }
                            r--;
                        }
                    }
                }
                markDirty();
                return;
            }
            case SEQUENTIAL -> {
                int next = playIndex + 1;

                while (next <= lastIndex) {
                    if (!playerInv.getStackInSlot(next).isEmpty()) {
                        playIndex = next;
                        markDirty();
                        return;
                    }
                    next++;
                }

                playIndex = firstIndex;
            }
            case LOOP -> {

            }
        }
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
        markDirty();
    }

    public void setPlayToClient(SongInfo info) {
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            SongInfo clone = info.clone();
            this.isPlay = true;
            this.markDirty();
            resolveUrlAsync(clone).thenAcceptAsync(resolved -> {
                try {
                    if (this.isRemoved()) return;
                    if (!this.isPlay) return;
                    this.setCurrentTime(resolved.songTime * 20 + 64);
                    this.markDirty();

                    String rawUrl = info.songUrl;
                    String url = resolved.songUrl;
                    PlayMusicPacket msg = new PlayMusicPacket(
                            worldPosition, url, rawUrl, resolved.songTime, resolved.songName, info.songId
                    );
                    ModNetwork.sendToNearby(level, worldPosition, msg);
                } catch (Exception e) {
                    MengSamaNetMusic.LOGGER.error("setPlayToClient error: {}", e.getMessage());
                }
            }, server);
        }
    }

    private static CompletableFuture<SongInfo> resolveUrlAsync(SongInfo info) {
        return CompletableFuture.supplyAsync(() -> {
            String url = info.songUrl;
            if (url == null || url.isBlank()) {
                return info;
            }

            if (com.mengsama.mod.mengsamanetmusic.api.MetingApi.isMetingUrl(url)) {
                MengSamaNetMusic.LOGGER.info("Using Meting API URL directly: {}", url);
                return info;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return info;
            }
            try {

                if (url.contains("music.163.com") && url.contains("outer/url")) {
                    long songId = extractSongId(url);
                    if (songId > 0) {
                        MengSamaNetMusic.LOGGER.info("Resolving song URL for id: {}", songId);

                        String resolvedUrl = resolveRedirectUrl(url);
                        if (resolvedUrl != null && !resolvedUrl.equals(url) && !resolvedUrl.contains("404")) {
                            info.songUrl = resolvedUrl;
                            MengSamaNetMusic.LOGGER.info("Resolved song URL via redirect: {} -> {}", url, resolvedUrl);
                            return info;
                        }

                        String metingUrl = com.mengsama.mod.mengsamanetmusic.api.MetingApi.getSongUrl(songId);
                        if (metingUrl != null && !metingUrl.isEmpty()) {
                            info.songUrl = metingUrl;
                            MengSamaNetMusic.LOGGER.info("Resolved VIP song URL via Meting API: {} -> {}", url, metingUrl);
                            return info;
                        }
                        MengSamaNetMusic.LOGGER.warn("Failed to resolve URL for song id: {}", songId);
                    }
                } else {

                    String resolvedUrl = resolveRedirectUrl(url);
                    if (resolvedUrl != null && !resolvedUrl.equals(url)) {
                        info.songUrl = resolvedUrl;
                    }
                }
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.warn("Failed to resolve URL for song: {}", url, e);
            }
            return info;
        });
    }

    private static long extractSongId(String url) {
        try {
            int idIdx = url.indexOf("id=");
            if (idIdx >= 0) {
                String sub = url.substring(idIdx + 3);
                int dot = sub.indexOf(".mp3");
                if (dot > 0) sub = sub.substring(0, dot);
                return Long.parseLong(sub);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private static String resolveRedirectUrl(String urlString) {

        java.util.Map<String, String> netEaseHeaders = MengSamaNetMusic.NET_EASE_API.getRequestPropertyData();
        String currentUrl = urlString;
        for (int i = 0; i < 5; i++) {
            try {
                URL url = new URI(currentUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                netEaseHeaders.forEach(connection::setRequestProperty);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (location != null) {
                        if (location.startsWith("/")) {
                            currentUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), location).toString();
                        } else if (location.startsWith("http://") || location.startsWith("https://")) {
                            currentUrl = location;
                        } else {
                            String base = url.getProtocol() + "://" + url.getHost();
                            if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443) {
                                base += ":" + url.getPort();
                            }
                            currentUrl = base + (location.startsWith("/") ? "" : "/") + location;
                        }
                        continue;
                    }
                }
                connection.disconnect();
                return currentUrl;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.warn("Failed to resolve redirect for {}: {}", currentUrl, e.getMessage());
                return currentUrl;
            }
        }
        return currentUrl;
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

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicPlayerBlockEntity te) {
        te.tickTime();
        if (0 < te.getCurrentTime() && te.getCurrentTime() < 16 && te.getCurrentTime() % 5 == 0) {
            if (blockState.getValue(CYCLE_DISABLE)) {
                te.setPlay(false);
                te.markDirty();
            } else {
                int prevPlayIndex = te.playIndex;
                te.advanceToNext();
                ItemStack currentCd = te.getCurrentCd();
                if (currentCd.isEmpty()) {
                    return;
                }
                SongInfo songInfo;
                if (currentCd.getItem() instanceof MusicListItem) {

                    if (te.playIndex == prevPlayIndex) {
                        MusicListItem.nextMusic(currentCd);
                    }
                    songInfo = MusicListItem.getSongInfo(currentCd);
                } else {
                    songInfo = MusicCDItem.getSongInfo(currentCd);
                }
                if (songInfo != null) {
                    te.setPlayToClient(songInfo);
                }
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public double getTick(Object blockEntity) {
        return 0;
    }
}
