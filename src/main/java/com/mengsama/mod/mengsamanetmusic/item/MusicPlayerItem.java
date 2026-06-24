package com.mengsama.mod.mengsamanetmusic.item;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.block.PortableMusicPlayerBlock;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock;
import com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerMenu;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayerPlayMusicPacket;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class MusicPlayerItem extends BlockItem {
    private static final int CD_SLOTS = 54;
    private static final String PLAY_INDEX_KEY = "PlayIndex";
    private static final String PLAY_MODE_KEY = "PlayMode";
    private static final String IS_PLAY_KEY = "IsPlay";
    private static final String CURRENT_TIME_KEY = "CurrentTime";

    public MusicPlayerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState targetState = level.getBlockState(hitResult.getBlockPos());
            if (targetState.getBlock() instanceof PortableMusicPlayerBlock ||
                    targetState.getBlock() instanceof MusicPlayerBlock) {
                return InteractionResultHolder.pass(stack);
            }
        }

        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (result.getResult().consumesAction()) {
            return result;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable("item.mengsamanetmusic.music_player");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, Player p) {
                    return new MusicPlayerMenu(windowId, playerInv);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (level.isClientSide) return;
        if (!isPlay(stack)) return;
        tickTime(stack);
        int currentTime = getCurrentTime(stack);
        if (0 < currentTime && currentTime < 16 && currentTime % 5 == 0) {
            int prevPlayIndex = getPlayIndex(stack);
            advanceToNext(stack);
            ItemStack cd = getCurrentCd(stack);
            if (cd.isEmpty()) {
                setPlay(stack, false);
                return;
            }
            SongInfo songInfo;
            if (cd.getItem() instanceof MusicListItem) {
                if (getPlayIndex(stack) == prevPlayIndex) {
                    MusicListItem.nextMusic(cd);
                }
                songInfo = MusicListItem.getSongInfo(cd);
            } else {
                songInfo = MusicCDItem.getSongInfo(cd);
            }
            if (songInfo != null && entity instanceof ServerPlayer sp) {
                setPlayToClient(stack, songInfo, sp);
            }
        }
    }

    public static void setPlayToClient(ItemStack stack, SongInfo info, ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        SongInfo clone = info.clone();
        setPlay(stack, true);
        resolveUrlAsync(clone).thenAcceptAsync(resolved -> {
            try {
                if (!isPlay(stack)) return;
                setCurrentTime(stack, resolved.songTime * 20 + 64);
                String url = resolved.songUrl;
                PlayerPlayMusicPacket msg = new PlayerPlayMusicPacket(
                        player.getId(), url, resolved.songTime, resolved.songName,
                        getPlayIndex(stack), resolved);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("setPlayToClient error: {}", e.getMessage());
            }
        }, serverLevel.getServer());
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

    public static void saveAllCdsToItem(ItemStack playerItem, NonNullList<ItemStack> cds) {
        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, cds);
        playerItem.addTagElement("Item", nbt);
    }

    public static void saveCdToItem(ItemStack playerItem, int slot, ItemStack cd) {
        NonNullList<ItemStack> items = NonNullList.withSize(CD_SLOTS, ItemStack.EMPTY);
        CompoundTag existing = playerItem.getTagElement("Item");
        if (existing != null) {
            ContainerHelper.loadAllItems(existing, items);
        }
        items.set(slot, cd);
        saveAllCdsToItem(playerItem, items);
    }

    public static int getPlayIndex(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(PLAY_INDEX_KEY)) {
            return stack.getTag().getInt(PLAY_INDEX_KEY);
        }
        return 0;
    }

    public static void setPlayIndex(ItemStack stack, int index) {
        stack.getOrCreateTag().putInt(PLAY_INDEX_KEY, index);
    }

    public static PlayMode getPlayMode(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(PLAY_MODE_KEY)) {
            return PlayMode.getMode(stack.getTag().getInt(PLAY_MODE_KEY));
        }
        return PlayMode.SEQUENTIAL;
    }

    public static void setPlayMode(ItemStack stack, PlayMode mode) {
        stack.getOrCreateTag().putInt(PLAY_MODE_KEY, mode.ordinal());
    }

    public static boolean isPlay(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(IS_PLAY_KEY)) {
            return stack.getTag().getBoolean(IS_PLAY_KEY);
        }
        return false;
    }

    public static void setPlay(ItemStack stack, boolean play) {
        stack.getOrCreateTag().putBoolean(IS_PLAY_KEY, play);
    }

    public static int getCurrentTime(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(CURRENT_TIME_KEY)) {
            return stack.getTag().getInt(CURRENT_TIME_KEY);
        }
        return 0;
    }

    public static void setCurrentTime(ItemStack stack, int time) {
        stack.getOrCreateTag().putInt(CURRENT_TIME_KEY, time);
    }

    public static void tickTime(ItemStack stack) {
        int ct = getCurrentTime(stack);
        if (ct > 0) {
            setCurrentTime(stack, ct - 1);
        }
    }

    @NotNull
    public static ItemStack findMusicPlayerItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof MusicPlayerItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static NonNullList<ItemStack> loadAllCds(ItemStack stack) {
        NonNullList<ItemStack> items = NonNullList.withSize(CD_SLOTS, ItemStack.EMPTY);
        CompoundTag nbt = stack.getTagElement("Item");
        if (nbt != null) {
            ContainerHelper.loadAllItems(nbt, items);
        }
        return items;
    }

    public static ItemStack getCurrentCd(ItemStack stack) {
        NonNullList<ItemStack> cds = loadAllCds(stack);
        int index = getPlayIndex(stack);
        if (index >= 0 && index < cds.size()) {
            ItemStack cd = cds.get(index);
            if (!cd.isEmpty()) return cd;
        }
        for (int i = 0; i < cds.size(); i++) {
            if (!cds.get(i).isEmpty()) {
                setPlayIndex(stack, i);
                return cds.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    public static void advanceToNext(ItemStack stack) {
        NonNullList<ItemStack> cds = loadAllCds(stack);
        int playIndex = getPlayIndex(stack);
        PlayMode mode = getPlayMode(stack);

        int firstIndex = findFirstNonEmpty(cds);
        int lastIndex = findLastNonEmpty(cds);
        if (firstIndex < 0) return;

        switch (mode) {
            case RANDOM -> {
                int count = 0;
                for (ItemStack cd : cds) { if (!cd.isEmpty()) count++; }
                if (count > 1) {
                    int r = new Random().nextInt(count);
                    for (int i = 0; i < CD_SLOTS; i++) {
                        if (!cds.get(i).isEmpty()) {
                            if (r == 0) { setPlayIndex(stack, i); return; }
                            r--;
                        }
                    }
                }
                return;
            }
            case SEQUENTIAL -> {
                int next = playIndex + 1;
                while (next <= lastIndex) {
                    if (!cds.get(next).isEmpty()) { setPlayIndex(stack, next); return; }
                    next++;
                }
                setPlayIndex(stack, firstIndex);
            }
            case LOOP -> { }
        }
    }

    private static int findFirstNonEmpty(NonNullList<ItemStack> cds) {
        for (int i = 0; i < CD_SLOTS; i++) {
            if (!cds.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static int findLastNonEmpty(NonNullList<ItemStack> cds) {
        for (int i = CD_SLOTS - 1; i >= 0; i--) {
            if (!cds.get(i).isEmpty()) return i;
        }
        return -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String text = "本模组由 MengSama0502 & niumadadi520 & YuZiJiang 合作制作\n感谢使用 感谢喜欢";
        Component rainbow = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            int color = rainbowColor(i, text.length());
            rainbow = rainbow.copy().append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(style -> style.withColor(TextColor.fromRgb(color))));
        }
        tooltip.add(rainbow);
    }

    private static int rainbowColor(int index, int total) {
        float hue = (float) index / total;
        float saturation = 1.0f;
        float value = 1.0f;
        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);
        float r, g, b;
        switch (h % 6) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            case 5: r = value; g = p; b = q; break;
            default: r = 1; g = 1; b = 1;
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
