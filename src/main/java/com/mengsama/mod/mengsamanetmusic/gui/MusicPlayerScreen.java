package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.AppleMusicApi;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseApi;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseSearchResult;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseSearchMetadataLoader;
import com.mengsama.mod.mengsamanetmusic.api.SearchGeneration;
import com.mengsama.mod.mengsamanetmusic.api.QqMusicUtils;
import com.mengsama.mod.mengsamanetmusic.api.QqSearchResult;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.api.VipCookieState;
import com.mengsama.mod.mengsamanetmusic.config.MusicPlayerUiConfig;
import com.mengsama.mod.mengsamanetmusic.client.MusicPlayerBackground;
import com.mengsama.mod.mengsamanetmusic.client.audio.ClientMusicPlayback;
import com.mengsama.mod.mengsamanetmusic.client.lyric.ClientLyricStore;
import com.mengsama.mod.mengsamanetmusic.client.lyric.PlaybackSeekUtil;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayerAddSongPacket;
import com.mengsama.mod.mengsamanetmusic.network.PlayerRemoveSongPacket;
import com.mengsama.mod.mengsamanetmusic.network.ReturnToMaidGuiPacket;
import com.mengsama.mod.mengsamanetmusic.network.SeekPlaybackPacket;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import com.mengsama.mod.mengsamanetmusic.util.PlaylistFilter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
public class MusicPlayerScreen extends AbstractContainerScreen<MusicPlayerMenu> {
    private static final int TAB_SEARCH = 0;
    private static final int TAB_PLAYLIST = 1;
    private static final int TAB_LYRICS = 2;
    private int currentTab = TAB_SEARCH;

    private static final int BG_PANEL = 0x99181830;
    private static final int BG_PANEL_LIGHT = 0x99202038;
    private static final int BG_CARD = 0x55222244;
    private static final int BG_CARD_HOVER = 0x88303055;
    private static final int ACCENT = 0xFF7C6FFF;
    private static final int ACCENT_BRIGHT = 0xFF9D8AFF;
    private static final int ACCENT_CYAN = 0xFF4ECDC4;
    private static final int ACCENT_DIM = 0x447C6FFF;
    private static final int BORDER = 0xFF2A2A45;
    private static final int BORDER_LIGHT = 0xFF3A3A5C;
    private static final int SLOT_BG = 0xFF16162A;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFB8B8CC;
    private static final int TEXT_DIM = 0xFF7A7A90;
    private static final int TEXT_ACCENT = 0xFF9D8AFF;
    private static final int STATUS_PLAYING = 0xFF4ECDC4;
    private static final int STATUS_STOPPED = 0xFFFF6B6B;
    private static final int PROGRESS_BG = 0xFF222238;

    private EditBox searchBox;
    private TransparentButton sourceButton;
    private TransparentButton searchButton;
    private TransparentButton qqLoginButton;
    private final ProviderAuthControls providerAuth = new ProviderAuthControls(
            ProviderAuthControls.Context.HELD_PLAYER, this, message -> this.statusMessage = message);
    /** 0=NetEase, 1=QQ, 2=Apple/iTunes preview. */
    private int searchSource = 0;
    private TransparentButton playButton;
    private TransparentButton stopButton;
    private TransparentButton nextButton;
    private TransparentButton prevButton;
    private TransparentButton modeButton;
    private TransparentButton broadcastButton;
    private TransparentButton tabSearchBtn;
    private TransparentButton tabPlaylistBtn;
    private TransparentButton tabLyricsBtn;
    private TransparentButton backToMaidButton;
    private boolean returningToMaid;
    private SearchResultList resultList;
    private PlaylistList playlistList;
    private LyricList lyricList;

    private boolean isSearching;
    private boolean draggingProgress;
    private int previewSeekSecond;
    private final SearchGeneration searchGeneration = new SearchGeneration();
    private Component statusMessage = Component.empty();
    private int lyricRefreshCounter = 0;
    private final PlayerLayoutEditor layoutEditor = new PlayerLayoutEditor("portable_item");

    private static final int TITLE_H = 24;
    private static final int SEARCH_Y = 30;
    private static final int SEARCH_H = 22;
    private static final int TAB_Y = 58;
    private static final int TAB_H = 20;
    private static final int CONTENT_Y = 84;
    private static final int CONTENT_H = 156;
    private static final int CONTROLS_Y = 244;
    private static final int CONTROLS_H = 28;
    private static final int PROGRESS_Y = 276;
    private static final int NOW_PLAYING_Y = 300;

    public MusicPlayerScreen(MusicPlayerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
        this.imageWidth = ui.screenWidth;
        this.imageHeight = ui.screenHeight;
    }

    @Override
    protected void init() {
        MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
        this.imageWidth = ui.screenWidth;
        this.imageHeight = ui.screenHeight;
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;
        this.layoutEditor.begin(cx, cy, this.imageWidth, this.imageHeight);
        if (this.menu.getBoundEntityId() != null) {
            int backWidth = Math.min(100, Math.max(72, this.imageWidth / 3));
            this.backToMaidButton = this.addRenderableWidget(TransparentButton.builder(
                            Component.translatable("gui.mengsamanetmusic.music_player.back_to_maid"), b -> returnToMaid())
                    .pos(cx + this.imageWidth - backWidth - 6, cy + 2).size(backWidth, 20).build());
        }
        int margin = ui.horizontalMargin;
        int gap = ui.controlGap;
        int sourceBtnW = ui.sourceButtonWidth;
        int searchBtnW = ui.searchButtonWidth;
        int loginBtnW = ui.qqLoginButtonWidth;
        int availableSearchW = this.imageWidth - margin * 2 - sourceBtnW - searchBtnW - loginBtnW - gap * 3;
        int searchBoxW = Math.max(80, Math.min(ui.searchBoxWidth, availableSearchW));
        this.sourceButton = this.addRenderableWidget(this.layoutEditor.register("source", TransparentButton.builder(getSourceButtonText(), b -> {
                    this.searchSource = (this.searchSource + 1) % 3;
                    searchGeneration.invalidate();
                    this.isSearching = false;
                    b.setMessage(getSourceButtonText());
                    this.statusMessage = Component.empty();
                    providerAuth.setProvider(searchSource);
                    updateTabVisibility();
                    if (this.resultList != null) this.resultList.setResults(Collections.emptyList());
                }).pos(cx + margin, cy + SEARCH_Y).size(sourceBtnW, SEARCH_H).build(), cx + margin - cx, cy + SEARCH_Y - cy));
        int searchBoxX = cx + margin + sourceBtnW + gap;
        this.searchBox = new EditBox(this.font, searchBoxX + 2, cy + SEARCH_Y + 2, searchBoxW - 4, 18,
                Component.translatable("gui.mengsamanetmusic.music_player.search_placeholder"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(50);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.mengsamanetmusic.music_player.search_hint")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        this.searchBox.setResponder(value -> {
            if (currentTab == TAB_PLAYLIST && playlistList != null) playlistList.applyUserFilter(value);
        });
        this.addWidget(this.searchBox);

        int searchBtnX = searchBoxX + searchBoxW + gap;
        this.searchButton = this.addRenderableWidget(this.layoutEditor.register("search", TransparentButton.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.search"), b -> performSearch())
                .pos(searchBtnX, cy + SEARCH_Y).size(searchBtnW, SEARCH_H).build(), searchBtnX - cx, cy + SEARCH_Y - cy));
        this.qqLoginButton = this.addRenderableWidget(this.layoutEditor.register("provider_auth",
                providerAuth.createButton(searchBtnX + searchBtnW + gap, cy + SEARCH_Y, loginBtnW, SEARCH_H),
                searchBtnX + searchBtnW + gap - cx, cy + SEARCH_Y - cy));
        providerAuth.setProvider(searchSource);

        int tabW = (this.imageWidth - margin * 2 - 8) / 3;
        this.tabSearchBtn = this.addRenderableWidget(this.layoutEditor.register("tab_search", TransparentButton.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_search"),
                        b -> switchTab(TAB_SEARCH))
                .pos(cx + margin, cy + TAB_Y).size(tabW, TAB_H).build(), cx + margin - cx, cy + TAB_Y - cy));
        this.tabPlaylistBtn = this.addRenderableWidget(this.layoutEditor.register("tab_playlist", TransparentButton.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_playlist"),
                        b -> switchTab(TAB_PLAYLIST))
                .pos(cx + 14 + tabW + 4, cy + TAB_Y).size(tabW, TAB_H).build(), cx + margin + tabW + 4 - cx, cy + TAB_Y - cy));
        this.tabLyricsBtn = this.addRenderableWidget(this.layoutEditor.register("tab_lyrics", TransparentButton.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_lyrics"),
                        b -> switchTab(TAB_LYRICS))
                .pos(cx + margin + (tabW + 4) * 2, cy + TAB_Y).size(tabW, TAB_H).build(), cx + margin + (tabW + 4) * 2 - cx, cy + TAB_Y - cy));

        int listWidth = this.imageWidth - margin * 2;
        this.resultList = new SearchResultList(this.minecraft, listWidth, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, ui.searchResultRowHeight);
        this.resultList.setLeftPos(cx + margin);
        this.addWidget(this.resultList);

        this.playlistList = new PlaylistList(this.minecraft, listWidth, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, ui.playlistRowHeight);
        this.playlistList.setLeftPos(cx + margin);
        this.addWidget(this.playlistList);

        this.lyricList = new LyricList(this.minecraft, listWidth, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, ui.lyricRowHeight);
        this.lyricList.setLeftPos(cx + margin);
        this.addWidget(this.lyricList);

        int btnY = cy + CONTROLS_Y;
        int buttonCount = this.menu.isPlayerHandContext() ? 6 : 5;
        int btnH = CONTROLS_H;
        int btnGap = 4;
        int btnW = Math.min(54, (this.imageWidth - margin * 2 - btnGap * (buttonCount - 1)) / buttonCount);
        int totalBtnW = btnW * buttonCount + btnGap * (buttonCount - 1);
        int btnStartX = cx + (this.imageWidth - totalBtnW) / 2;

        this.prevButton = this.addRenderableWidget(this.layoutEditor.register("previous", TransparentButton.builder(
                        Component.literal("\u25C0\u25C0"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_PREV))
                .pos(btnStartX, btnY).size(btnW, btnH).build(), btnStartX - cx, btnY - cy));
        this.playButton = this.addRenderableWidget(this.layoutEditor.register("play_pause", TransparentButton.builder(
                        getPlayButtonText(), b -> handleButtonClick(MusicPlayerMenu.BUTTON_PLAY))
                .pos(btnStartX + (btnW + btnGap), btnY).size(btnW, btnH).build(), btnStartX + (btnW + btnGap) - cx, btnY - cy));
        this.stopButton = this.addRenderableWidget(this.layoutEditor.register("stop", TransparentButton.builder(
                        Component.literal("\u25A0"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_STOP))
                .pos(btnStartX + (btnW + btnGap) * 2, btnY).size(btnW, btnH).build(), btnStartX + (btnW + btnGap) * 2 - cx, btnY - cy));
        this.nextButton = this.addRenderableWidget(this.layoutEditor.register("next", TransparentButton.builder(
                        Component.literal("\u25B6\u25B6"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_NEXT))
                .pos(btnStartX + (btnW + btnGap) * 3, btnY).size(btnW, btnH).build(), btnStartX + (btnW + btnGap) * 3 - cx, btnY - cy));
        this.modeButton = this.addRenderableWidget(this.layoutEditor.register("mode", TransparentButton.builder(
                        getModeButtonText(this.menu.getPlayMode()), b -> handleButtonClick(MusicPlayerMenu.BUTTON_MODE))
                .pos(btnStartX + (btnW + btnGap) * 4, btnY).size(btnW, btnH).build(), btnStartX + (btnW + btnGap) * 4 - cx, btnY - cy));
        if (this.menu.isPlayerHandContext()) {
            this.broadcastButton = this.addRenderableWidget(this.layoutEditor.register("broadcast", TransparentButton.builder(
                            getBroadcastButtonText(), b -> handleButtonClick(MusicPlayerMenu.BUTTON_BROADCAST))
                    .pos(btnStartX + (btnW + btnGap) * 5, btnY).size(btnW, btnH).build(),
                    btnStartX + (btnW + btnGap) * 5 - cx, btnY - cy));
        }

        switchTab(TAB_SEARCH);
    }

    private void returnToMaid() {
        if (returningToMaid || this.menu.getBoundEntityId() == null
                || this.menu.getBoundRuntimeEntityId() < 0 || this.menu.getBoundInstanceId() == null) return;
        returningToMaid = true;
        ModNetwork.CHANNEL.sendToServer(new ReturnToMaidGuiPacket(this.menu.getBoundEntityId(),
                this.menu.getBoundRuntimeEntityId(), this.menu.getBoundInstanceId()));
    }

    @Override
    public void onClose() {
        if (this.menu.getBoundEntityId() != null && !returningToMaid) {
            // Esc/关闭先正常退出当前播放器容器，再单次请求恢复原女仆 GUI。
            // removed() 不发包，因此服务器切换界面时不会循环自动重开。
            super.onClose();
            returnToMaid();
            return;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        // 仅清理当前界面；禁止在服务器切换容器触发的 removed() 中再次发返回包。
        searchGeneration.invalidate();
        isSearching = false;
        MusicPlayerBackground.close();
        super.removed();
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        updateTabVisibility();
        if (this.tabSearchBtn != null) this.tabSearchBtn.setSelected(tab == TAB_SEARCH);
        if (this.tabPlaylistBtn != null) this.tabPlaylistBtn.setSelected(tab == TAB_PLAYLIST);
        if (this.tabLyricsBtn != null) this.tabLyricsBtn.setSelected(tab == TAB_LYRICS);

        if (this.playlistList != null && tab == TAB_PLAYLIST) {
            this.playlistList.refresh();
        }
        if (this.lyricList != null && tab == TAB_LYRICS) {
            this.lyricList.refresh();
        }
    }

    private void handleButtonClick(int buttonId) {
        // 本地拥有声源的手持设备先直接暂停 OpenAL，不等待客户端->服务器->客户端往返。
        if (buttonId == MusicPlayerMenu.BUTTON_PLAY) {
            String target = ClientMusicPlayback.authoritativeTarget(this.menu.getTargetId());
            boolean paused = ClientMusicPlayback.isPaused(target);
            if (this.menu.isPlaying() || paused) ClientMusicPlayback.setPaused(target, !paused);
        }
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updateTabVisibility() {
        // Selection lists are rendered and receive input only through the current-tab branches below.
        boolean onlineSearch = currentTab == TAB_SEARCH;
        boolean searchVisible = onlineSearch || currentTab == TAB_PLAYLIST;
        if (this.searchBox != null) {
            this.searchBox.visible = searchVisible;
            this.searchBox.setHint(Component.translatable(onlineSearch
                    ? "gui.mengsamanetmusic.music_player.search_hint"
                    : "gui.mengsamanetmusic.music_player.playlist_filter_hint")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        if (this.sourceButton != null) this.sourceButton.visible = onlineSearch;
        if (this.searchButton != null) this.searchButton.visible = onlineSearch;
        // NetEase never needs an account button. QQ keeps the original QR/status/logout action;
        // Apple shows only the safe capability explanation unless compliant MusicKit is configured.
        if (this.qqLoginButton != null) this.qqLoginButton.visible = onlineSearch && searchSource != 0;
    }

    private boolean isPlaying() {
        return this.menu.isPlaying();
    }

    private Component getSourceButtonText() {
        return Component.literal(switch (searchSource) { case 1 -> "QQ音乐"; case 2 -> "Apple"; default -> "网易云"; });
    }

    private void performSearch() {
        if (this.searchBox == null) return;
        String query = this.searchBox.getValue().trim();
        if (query.isEmpty()) {
            this.statusMessage = Component.translatable("gui.mengsamanetmusic.music_player.search_empty");
            return;
        }
        this.isSearching = true;
        int selectedSource = this.searchSource;
        boolean qqSearch = selectedSource == 1;
        boolean appleSearch = selectedSource == 2;
        String queryToken = (qqSearch ? "qq:" : appleSearch ? "apple:" : "netease:") + query;
        long generation = searchGeneration.begin(queryToken);
        this.statusMessage = Component.translatable("gui.mengsamanetmusic.search.searching");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        if (qqSearch) {
                            List<NetEaseSearchResult> results = new ArrayList<>();
                            for (QqSearchResult result : QqMusicUtils.search(query)) {
                                results.add(new NetEaseSearchResult(result.getId(), result.getTitle(),
                                        result.getSinger(), result.isVip(), "qq",
                                        result.getAlbumMid(), result.getCoverUrl(), result.getAlbumName(), result.getDuration()));
                            }
                            return results;
                        }
                        if (appleSearch) return AppleMusicApi.search(query);
                        String json = MengSamaNetMusic.NET_EASE_API.search(query, 1, 30);
                        return parseSearchResults(json);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, Util.backgroundExecutor())
                .whenComplete((results, error) -> Minecraft.getInstance().execute(() -> {
                    if (!searchGeneration.isCurrent(generation, queryToken)) return;
                    this.isSearching = false;
                    if (error != null) {
                        this.statusMessage = Component.translatable("gui.mengsamanetmusic.search.search_failed");
                        if (this.resultList != null) this.resultList.setResults(Collections.emptyList());
                    } else {
                        switchTab(TAB_SEARCH);
                        if (this.resultList != null) {
                            this.resultList.setResults(results);
                            this.resultList.setScrollAmount(0.0D);
                        }
                        if (results == null || results.isEmpty()) {
                            this.statusMessage = Component.translatable("gui.mengsamanetmusic.search.no_result");
                        } else {
                            this.statusMessage = Component.empty();
                            if (!qqSearch && !appleSearch) hydrateSearchCovers(results, generation, queryToken);
                        }
                    }
                }));
    }

    private List<NetEaseSearchResult> parseSearchResults(String json) {
        return NetEaseApi.parseSearchResults(json);
    }

    private void hydrateSearchCovers(List<NetEaseSearchResult> results, long generation, String queryToken) {
        NetEaseSearchMetadataLoader.hydrateMissing(results, MengSamaNetMusic.NET_EASE_API)
                .whenComplete((hydrated, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null || !searchGeneration.isCurrent(generation, queryToken)) return;
                    if (this.resultList != null) this.resultList.replaceResults(hydrated);
                }));
    }

    private void onSearchResultClicked(NetEaseSearchResult result, boolean playNow) {
        if (result == null) return;
        this.statusMessage = Component.translatable("gui.mengsamanetmusic.search.loading");
        CompletableFuture.supplyAsync(() -> {
            try {
                SongInfo song;
                if (result.isApple()) {
                    song = AppleMusicApi.toSong(result);
                } else if (result.isQq()) {
                    song = QqMusicUtils.resolveSong(result.getSongId(), VipCookieState.getEffectiveVipCookie(), 1);
                    if (song != null) {
                        song.source = "qq";
                        song.artists.clear();
                        if (result.getArtistName() != null && !result.getArtistName().isBlank()) {
                            song.artists.add(result.getArtistName());
                        }
                        song.providerId = result.getSongId();
                        // Search metadata is authoritative when present, but an empty search
                        // album must not erase the detail endpoint metadata resolved above.
                        if (result.getAlbumMid() != null && !result.getAlbumMid().isBlank()) {
                            song.albumMid = result.getAlbumMid();
                        }
                        if (result.getCoverUrl() != null && !result.getCoverUrl().isBlank()) {
                            song.coverUrl = result.getCoverUrl();
                            song.picUrl = result.getCoverUrl();
                        }
                        if (!result.getAlbumName().isBlank()) song.albumName = result.getAlbumName();
                        if (song.songTime <= 0) song.songTime = result.getDuration();
                        song.normalizeIdentity();
                    }
                } else {
                    song = MengSamaNetMusic.NET_EASE_API.get163Song(result);
                }
                return song;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to fetch song info", e);
                return null;
            }
        }, Util.backgroundExecutor()).thenAccept(song -> Minecraft.getInstance().execute(() -> {
            if (song != null && song.isValid()) {
                ModNetwork.CHANNEL.sendToServer(new PlayerAddSongPacket(song, playNow));
                this.statusMessage = playNow
                        ? Component.literal("\u25B6 " + song.songName).withStyle(ChatFormatting.GREEN)
                        : Component.literal("\u2713 " + song.songName).withStyle(ChatFormatting.AQUA);
            } else {
                this.statusMessage = Component.translatable("gui.mengsamanetmusic.search.get_info_error");
            }
        }));
    }

    private Component getPlayButtonText() {
        return Component.literal(isPlaying() ? "\u2016" : "\u25B6");
    }

    private Component getModeButtonText(PlayMode mode) {
        return switch (mode) {
            case LOOP -> Component.literal("单曲");
            case SEQUENTIAL -> Component.literal("列表");
            case RANDOM -> Component.literal("随机");
        };
    }

    private Component getBroadcastButtonText() {
        return Component.translatable(this.menu.isBroadcast()
                ? "gui.mengsamanetmusic.music_player.broadcast_on"
                : "gui.mengsamanetmusic.music_player.broadcast_off");
    }

    private SongInfo getSongInfoFromCd(ItemStack cd) {
        return cd.getItem() instanceof MusicListItem ? MusicListItem.getSongInfo(cd) : null;
    }

    static boolean samePlaylist(List<SongInfo> leftSongs, List<Integer> leftSlots,
                                List<SongInfo> rightSongs, List<Integer> rightSlots) {
        if (!leftSlots.equals(rightSlots) || leftSongs.size() != rightSongs.size()) return false;
        for (int i = 0; i < leftSongs.size(); i++) {
            SongInfo left = leftSongs.get(i);
            SongInfo right = rightSongs.get(i);
            if (!left.identityKey().equals(right.identityKey())
                    || !java.util.Objects.equals(left.songName, right.songName)
                    || left.songTime != right.songTime
                    || !java.util.Objects.equals(left.artists, right.artists)) return false;
        }
        return true;
    }

    private String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        renderBackground(graphics);
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
        MusicPlayerBackground.renderCover(graphics, x, y, w, h, ui.background());

        graphics.fill(x, y, x + w, y + TITLE_H, ui.panel());
        graphics.fill(x, y + TITLE_H - 1, x + w, y + TITLE_H, ui.accent());
        graphics.fill(x, y + TITLE_H, x + w, y + TITLE_H + 2, (0x44 << 24) | ui.themeRgb());

        graphics.fill(x + 8, y + TITLE_H + 2, x + w - 8, y + SEARCH_Y + SEARCH_H + 4, ui.panelSurface());
        int availableSearchW = w - ui.horizontalMargin * 2 - ui.sourceButtonWidth - ui.searchButtonWidth - ui.qqLoginButtonWidth - ui.controlGap * 3;
        int searchBoxW = Math.max(80, Math.min(ui.searchBoxWidth, availableSearchW));
        int searchBoxX = x + ui.horizontalMargin + ui.sourceButtonWidth + ui.controlGap;
        graphics.fill(searchBoxX, y + SEARCH_Y, searchBoxX + searchBoxW, y + SEARCH_Y + SEARCH_H, ui.popupSurface());
        graphics.renderOutline(searchBoxX, y + SEARCH_Y, searchBoxW, SEARCH_H, ui.border());
        if (this.searchBox != null && this.searchBox.isFocused()) {
            graphics.renderOutline(searchBoxX, y + SEARCH_Y, searchBoxW, SEARCH_H, ui.accent());
        }

        graphics.fill(x + 8, y + TAB_Y - 2, x + w - 8, y + TAB_Y + TAB_H + 2, ui.panelSurface());

        graphics.fill(x + 8, y + CONTENT_Y - 2, x + w - 8, y + CONTENT_Y + CONTENT_H + 2, ui.listSurface());
        graphics.renderOutline(x + 8, y + CONTENT_Y - 2, w - 16, CONTENT_H + 4, ui.border());
        graphics.fill(x + 9, y + CONTENT_Y - 1, x + w - 9, y + CONTENT_Y, ACCENT_DIM);

        graphics.fill(x + 8, y + CONTROLS_Y - 4, x + w - 8, y + CONTROLS_Y + CONTROLS_H + 4, ui.panelSurface());
        graphics.renderOutline(x + 8, y + CONTROLS_Y - 4, w - 16, CONTROLS_H + 8, ui.border());

        graphics.fill(x + 8, y + PROGRESS_Y - 2, x + w - 8, y + PROGRESS_Y + 20, ui.panelSurface());

        graphics.fill(x + 8, y + NOW_PLAYING_Y, x + w - 8, y + NOW_PLAYING_Y + 24, ui.popupSurface());
        int statusColor = isPlaying() ? STATUS_PLAYING : STATUS_STOPPED;
        graphics.fill(x + 8, y + NOW_PLAYING_Y, x + 10, y + NOW_PLAYING_Y + 24, statusColor);

        int sepY = y + MusicPlayerMenu.INV_Y - 10;
        for (int i = 0; i < w - 16; i++) {
            float ratio = (float) i / (w - 16);
            int alpha = (int) (0x66 * (1 - Math.abs(ratio - 0.5) * 2));
            graphics.fill(x + 8 + i, sepY, x + 8 + i + 1, sepY + 1, (alpha << 24) | ui.themeRgb());
        }

        int invLeft = x + MusicPlayerMenu.INV_X - 1;
        int invTop = y + MusicPlayerMenu.INV_Y - 1;
        int invWidth = 9 * 18 + 2;
        int invHeight = 3 * 18 + 2;
        graphics.fill(invLeft, invTop, invLeft + invWidth, invTop + invHeight, ui.panelSurface());
        graphics.renderOutline(invLeft, invTop, invWidth, invHeight, ui.border());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                graphics.renderOutline(invLeft + 1 + col * 18, invTop + 1 + row * 18, 18, 18, BORDER);
            }
        }

        int hotbarTop = y + MusicPlayerMenu.HOTBAR_Y - 1;
        graphics.fill(invLeft, hotbarTop, invLeft + invWidth, hotbarTop + 20, ui.panelSurface());
        graphics.renderOutline(invLeft, hotbarTop, invWidth, 20, ui.border());
        for (int col = 0; col < 9; col++) {
            graphics.renderOutline(invLeft + 1 + col * 18, hotbarTop + 1, 18, 18, BORDER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.playButton != null) this.playButton.setMessage(getPlayButtonText());
        if (this.modeButton != null) this.modeButton.setMessage(getModeButtonText(this.menu.getPlayMode()));
        if (this.broadcastButton != null) this.broadcastButton.setMessage(getBroadcastButtonText());

        super.render(graphics, mouseX, mouseY, partialTicks);

        int x = this.leftPos;
        int y = this.topPos;

        MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
        graphics.drawString(font, Component.literal("\u266A"), x + 10, y + 7, ui.accent(), false);
        graphics.drawString(font, this.title, x + 22, y + 7, ui.primaryText(), false);

        if (this.searchBox != null) this.searchBox.render(graphics, mouseX, mouseY, partialTicks);

        if (currentTab == TAB_SEARCH) {
            if (this.resultList != null) this.resultList.render(graphics, mouseX, mouseY, partialTicks);
            if (!this.statusMessage.getString().isEmpty()) {
                graphics.drawCenteredString(font, this.statusMessage,
                        x + this.imageWidth / 2, y + CONTENT_Y + CONTENT_H - 14, TEXT_SECONDARY);
            }
        } else if (currentTab == TAB_PLAYLIST) {
            if (this.playlistList != null) this.playlistList.render(graphics, mouseX, mouseY, partialTicks);
        } else if (currentTab == TAB_LYRICS) {
            if (this.lyricList != null) this.lyricList.render(graphics, mouseX, mouseY, partialTicks);
        }

        renderProgressBar(graphics, x, y);

        renderNowPlaying(graphics, x, y);

        graphics.drawString(font, Component.translatable("container.inventory"),
                x + 12, y + MusicPlayerMenu.INV_Y - 10, TEXT_SECONDARY, false);

        this.layoutEditor.render(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y) {
        int barX = x + 14;
        int barY = y + PROGRESS_Y + 4;
        int barW = this.imageWidth - 28;
        int barH = 6;

        graphics.fill(barX, barY, barX + barW, barY + barH, PROGRESS_BG);
        graphics.renderOutline(barX, barY, barW, barH, BORDER);

        int tick = getPlayingTick();
        SongInfo playingInfo = getPlayingSongInfo();
        if (playingInfo != null && playingInfo.songTime > 0 && tick >= 0) {
            int shownSecond = draggingProgress ? previewSeekSecond : PlaybackSeekUtil.secondAtTick(tick);
            float progress = Math.max(0, Math.min(1, (float) shownSecond / playingInfo.songTime));
            int fillW = (int) (barW * progress);
            if (fillW > 0) {
                MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
                graphics.fill(barX, barY, barX + fillW, barY + barH, ui.accent());
                graphics.fill(barX, barY, barX + fillW, barY + 1, 0x66FFFFFF);
                int handleX = barX + fillW - 2;
                graphics.fill(handleX, barY - 1, handleX + 4, barY + barH + 1, ui.secondaryAccent());
            }
            String currentTime = formatTime(shownSecond);
            String totalTime = formatTime(playingInfo.songTime);
            graphics.drawString(font, currentTime, barX, barY + barH + 3, TEXT_DIM, false);
            int totalW = font.width(totalTime);
            graphics.drawString(font, totalTime, barX + barW - totalW, barY + barH + 3, TEXT_DIM, false);
        }
    }

    private void renderNowPlaying(GuiGraphics graphics, int x, int y) {
        SongInfo playingInfo = getPlayingSongInfo();
        int infoY = y + NOW_PLAYING_Y + 4;
        boolean playing = isPlaying();

        String statusIcon = playing ? "\u25B6" : "\u25A0";
        int statusColor = playing ? STATUS_PLAYING : STATUS_STOPPED;
        graphics.drawString(font, statusIcon, x + 14, infoY, statusColor, false);
        Component statusText = playing
                ? Component.translatable("gui.mengsamanetmusic.music_player.playing")
                : Component.translatable("gui.mengsamanetmusic.music_player.stopped");
        graphics.drawString(font, statusText, x + 26, infoY, statusColor, false);

        if (playingInfo != null && playingInfo.songName != null) {
            String name = playingInfo.songName;
            int maxW = this.imageWidth - 80;
            if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - 8) + "...";
            graphics.drawString(font, name, x + 14, infoY + 12, TEXT_PRIMARY, false);

            String sourceName = SongInfo.getSourceDisplayName(playingInfo.source);
            int sourceColor = SongInfo.getSourceColor(playingInfo.source);
            int sourceX = x + 14 + font.width(name) + 6;
            int sourceW = font.width(sourceName) + 4;
            if (sourceX + sourceW < x + this.imageWidth - 14) {
                graphics.fill(sourceX, infoY + 11, sourceX + sourceW, infoY + 21, 0x55000000);
                graphics.drawString(font, sourceName, sourceX + 2, infoY + 12, sourceColor, false);
            }
        }
    }

    private String getPlaybackTarget() {
        return ClientMusicPlayback.authoritativeTarget(this.menu.getTargetId());
    }

    private int getPlayingTick() {
        return ClientMusicPlayback.getTick(getPlaybackTarget());
    }

    private SongInfo getPlayingSongInfo() {
        SongInfo active = ClientMusicPlayback.getSongInfo(getPlaybackTarget());
        // The GUI can open before the client sound object is registered (and briefly between an
        // expired-URL failure and its refreshed replacement). Keep lyrics bound to the menu's
        // authoritative current song instead of clearing the lyric slot during that window.
        return ClientLyricStore.selectGuiSong(active, this.menu.isPlaying(),
                this.menu.getSongInfo(this.menu.getPlayIndex()));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && updateSeekPreview(mouseX, mouseY)) {
            draggingProgress = true;
            return true;
        }
        if (this.layoutEditor.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.searchBox != null && this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        }

        if (currentTab == TAB_SEARCH && this.resultList != null) {
            if (this.resultList.mouseClicked(mouseX, mouseY, button)) return true;
        } else if (currentTab == TAB_PLAYLIST && this.playlistList != null) {
            if (this.playlistList.mouseClicked(mouseX, mouseY, button)) return true;
        } else if (currentTab == TAB_LYRICS && this.lyricList != null) {
            if (this.lyricList.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingProgress) {
            updateSeekPreview(mouseX, mouseY);
            return true;
        }
        if (this.layoutEditor.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingProgress) {
            updateSeekPreview(mouseX, mouseY);
            draggingProgress = false;
            String targetId = getPlaybackTarget();
            ClientMusicPlayback.seekImmediately(targetId, previewSeekSecond);
            int entityId = this.menu.getBoundRuntimeEntityId() >= 0 ? this.menu.getBoundRuntimeEntityId()
                    : this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getId() : -1;
            if (entityId >= 0) ModNetwork.CHANNEL.sendToServer(
                    new SeekPlaybackPacket(entityId, null, getPlaybackTarget(), previewSeekSecond,
                            getPlayingSongInfo() == null ? "" : getPlayingSongInfo().identityKey()));
            return true;
        }
        if (this.layoutEditor.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean updateSeekPreview(double mouseX, double mouseY) {
        SongInfo song = getPlayingSongInfo();
        int barX = this.leftPos + 14;
        int barY = this.topPos + PROGRESS_Y + 4;
        int barW = this.imageWidth - 28;
        if (song == null || song.songTime <= 0 || mouseY < barY - 5 || mouseY > barY + 11) return false;
        previewSeekSecond = PlaybackSeekUtil.secondAtFraction((mouseX - barX) / barW, song.songTime);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {

        if (currentTab == TAB_SEARCH && this.resultList != null) {
            if (this.resultList.mouseScrolled(mouseX, mouseY, delta)) return true;
        } else if (currentTab == TAB_PLAYLIST && this.playlistList != null) {
            if (this.playlistList.mouseScrolled(mouseX, mouseY, delta)) return true;
        } else if (currentTab == TAB_LYRICS && this.lyricList != null) {
            if (this.lyricList.mouseScrolled(mouseX, mouseY, delta)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.layoutEditor.keyPressed(keyCode, modifiers, this::reinitializeLayout)) return true;
        if (keyCode == InputConstants.KEY_ESCAPE && this.menu.getBoundEntityId() != null) {
            returnToMaid();
            return true;
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft != null && this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            if (this.searchBox != null && this.searchBox.isFocused()) return true;
            this.onClose();
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && this.searchBox != null && this.searchBox.isFocused()) {
            performSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void reinitializeLayout() {
        String value = this.searchBox != null ? this.searchBox.getValue() : "";
        int tab = this.currentTab;
        this.clearWidgets();
        this.init();
        if (this.searchBox != null) this.searchBox.setValue(value);
        switchTab(tab);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        providerAuth.tick();
        if (this.searchBox != null) this.searchBox.tick();
        if (currentTab == TAB_LYRICS && this.lyricList != null) {
            // The active sound tick is the same authoritative clock used by placed players and the HUD.
            // Refresh every client tick so timestamp boundaries highlight and auto-follow without 500 ms lag.
            lyricRefreshCounter = 0;
            this.lyricList.refresh();
        }

        if (currentTab == TAB_PLAYLIST && this.playlistList != null) {
            lyricRefreshCounter++;
            if (lyricRefreshCounter >= 20) {
                lyricRefreshCounter = 0;
                this.playlistList.refresh();
            }
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String searchValue = this.searchBox != null ? this.searchBox.getValue() : "";
        int oldTab = this.currentTab;
        super.resize(minecraft, width, height);
        if (this.searchBox != null) this.searchBox.setValue(searchValue);
        switchTab(oldTab);
    }

    private void removeSong(int slot) {
        ModNetwork.CHANNEL.sendToServer(new PlayerRemoveSongPacket(slot));
        if (this.playlistList != null) this.playlistList.refresh();
    }

    private class SearchResultList extends ObjectSelectionList<SearchResultList.Entry> {
        public SearchResultList(Minecraft mc, int w, int h, int top, int bottom, int itemH) {
            super(mc, w, h, top, bottom, itemH);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        public int getRowWidth() { return this.getWidth() - 12; }

        @Override
        protected int getScrollbarPosition() { return this.getLeft() + this.getWidth() - 6; }

        public void setResults(List<NetEaseSearchResult> results) {
            this.clearEntries();
            if (results == null) return;
            for (NetEaseSearchResult r : results) this.addEntry(new Entry(r));
        }

        public void replaceResults(List<NetEaseSearchResult> results) {
            double scroll = this.getScrollAmount();
            setResults(results);
            this.setScrollAmount(scroll);
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final NetEaseSearchResult result;
            private int rowX, rowY, rowW, rowH;
            public Entry(NetEaseSearchResult r) { this.result = r; }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                               int mx, int my, boolean hovered, float pt) {
                this.rowX = x; this.rowY = y; this.rowW = ew; this.rowH = eh;
                MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
                SongRowRenderer.renderSearch(g, font, result, x, y, ew, eh, hovered, ui.listSurface(), ui.listHoverSurface());
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button == 0 && SongRowRenderer.hitAction(mx, my, rowX, rowY, rowW, rowH)) {
                    onSearchResultClicked(result, false);
                    return true;
                }
                return false;
            }

            @Override
            public @NotNull Component getNarration() { return Component.literal(result.getDisplayText()); }
        }
    }

    private class PlaylistList extends ObjectSelectionList<PlaylistList.Entry> {
        private final List<SongInfo> songs = new ArrayList<>();
        private final List<Integer> slotIndices = new ArrayList<>();

        public PlaylistList(Minecraft mc, int w, int h, int top, int bottom, int itemH) {
            super(mc, w, h, top, bottom, itemH);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        public int getRowWidth() { return this.getWidth() - 12; }

        @Override
        protected int getScrollbarPosition() { return this.getLeft() + this.getWidth() - 6; }

        public void refresh() {
            List<SongInfo> newSongs = new ArrayList<>();
            List<Integer> newSlots = new ArrayList<>();
            double oldScroll = this.getScrollAmount();
            int anchorSlot = visibleSlotAt(oldScroll);
            double anchorOffset = oldScroll - Math.floor(oldScroll / rowHeight()) * rowHeight();
            if (minecraft != null && minecraft.player != null) {
                // 必须读取当前菜单绑定的权威设备；女仆上下文中玩家背包里的第一个播放器并非目标设备。
                ItemStack item = menu.getDevice();
                if (!item.isEmpty()) {
                    var cds = MusicPlayerItem.loadAllCds(item);
                    for (int i = 0; i < cds.size(); i++) {
                        if (!cds.get(i).isEmpty()) {
                            SongInfo info = getSongInfoFromCd(cds.get(i));
                            if (info != null) {
                                newSongs.add(info);
                                newSlots.add(i);
                            }
                        }
                    }
                }
            }
            if (samePlaylist(songs, slotIndices, newSongs, newSlots)) return;
            songs.clear();
            songs.addAll(newSongs);
            slotIndices.clear();
            slotIndices.addAll(newSlots);
            applyFilter(searchBox == null ? "" : searchBox.getValue(), oldScroll, anchorSlot, anchorOffset);
        }

        private int rowHeight() {
            return Math.max(1, MusicPlayerUiConfig.get().playlistRowHeight);
        }

        private int visibleSlotAt(double scroll) {
            int entryIndex = Math.max(0, (int) Math.floor(scroll / rowHeight()));
            return entryIndex < children().size() ? children().get(entryIndex).realSlot : -1;
        }

        public void applyUserFilter(String query) {
            applyFilter(query, 0.0D, -1, 0.0D);
        }

        private void applyFilter(String query, double fallbackScroll, int anchorSlot, double anchorOffset) {
            this.clearEntries();
            int anchorIndex = -1;
            for (PlaylistFilter.Match match : PlaylistFilter.filter(songs, slotIndices, query)) {
                if (match.slotIndex() == anchorSlot) anchorIndex = getItemCount();
                this.addEntry(new Entry(match.song(), match.slotIndex()));
            }
            double restored = anchorIndex >= 0 ? anchorIndex * rowHeight() + anchorOffset : fallbackScroll;
            this.setScrollAmount(Math.max(0.0D, restored));
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final SongInfo song;
            private final int realSlot;
            private int rowX, rowY, rowW, rowH;

            public Entry(SongInfo song, int realSlot) {
                this.song = song;
                this.realSlot = realSlot;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                               int mx, int my, boolean hovered, float pt) {
                this.rowX = x; this.rowY = y; this.rowW = ew; this.rowH = eh;
                boolean isCurrent = realSlot == menu.getPlayIndex();
                MusicPlayerUiConfig.Values ui = MusicPlayerUiConfig.get();
                SongRowRenderer.renderPlaylist(g, font, song, realSlot, x, y, ew, eh,
                        hovered, isCurrent, ui.listSurface(), ui.listHoverSurface());
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button == 0 && SongRowRenderer.hitAction(mx, my, rowX, rowY, rowW, rowH)) {
                    removeSong(realSlot);
                    return true;
                }
                if (button == 0) {
                    handleButtonClick(MusicPlayerMenu.BUTTON_SELECT_BASE + realSlot);
                    return true;
                }
                return false;
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.literal(song.songName != null ? song.songName : "Unknown");
            }
        }
    }

    private class LyricList extends ObjectSelectionList<LyricList.Entry> {
        private String renderedIdentity = "";
        private long renderedGeneration = Long.MIN_VALUE;
        private ClientLyricStore.State renderedState;
        private int currentLine = Integer.MIN_VALUE;
        private boolean autoFollow = true;

        public LyricList(Minecraft mc, int w, int h, int top, int bottom, int itemH) {
            super(mc, w, h, top, bottom, itemH);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override public int getRowWidth() { return this.getWidth() - 16; }
        @Override protected int getScrollbarPosition() { return this.getLeft() + this.getWidth() - 6; }

        public void refresh() {
            String target = getPlaybackTarget();
            SongInfo song = getPlayingSongInfo();
            ClientLyricStore.Snapshot snapshot = ClientLyricStore.bind(target, song);
            int tick = getPlayingTick();
            int line = ClientLyricStore.lineIndexAtTick(snapshot.data(), tick);
            boolean identityChanged = !snapshot.identity().equals(renderedIdentity) || snapshot.generation() != renderedGeneration;
            boolean stateChanged = snapshot.state() != renderedState;
            if (!identityChanged && !stateChanged && line == currentLine) return;

            double oldScroll = getScrollAmount();
            renderedIdentity = snapshot.identity();
            renderedGeneration = snapshot.generation();
            renderedState = snapshot.state();
            currentLine = line;
            this.clearEntries();
            if (snapshot.state() == ClientLyricStore.State.LOADING) {
                this.addEntry(new Entry("歌词加载中…", false));
            } else if (snapshot.state() == ClientLyricStore.State.FAILED) {
                this.addEntry(new Entry("歌词加载失败", false));
            } else if (snapshot.data().lines().isEmpty()) {
                String empty = song != null && "apple".equals(song.source) ? "Apple Music 暂无歌词" : "暂无歌词";
                this.addEntry(new Entry(empty, false));
            } else {
                int index = 0;
                for (String text : snapshot.data().lines().values()) this.addEntry(new Entry(text, index++ == currentLine));
            }
            if (identityChanged) {
                autoFollow = true;
                if (currentLine >= 0 && currentLine < getItemCount()) centerScrollOn(getEntry(currentLine));
                else setScrollAmount(0);
            } else if (autoFollow && currentLine >= 0 && currentLine < getItemCount()) {
                centerScrollOn(getEntry(currentLine));
            } else {
                setScrollAmount(oldScroll);
            }
        }

        @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            boolean handled = super.mouseScrolled(mouseX, mouseY, delta);
            if (handled) autoFollow = false;
            return handled;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String text;
            private final boolean current;
            Entry(String text, boolean current) { this.text = text == null ? "" : text; this.current = current; }
            @Override public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                                         int mx, int my, boolean hovered, float pt) {
                if (current) { g.fill(x, y - 1, x + ew, y + eh, 0x444ECDC4); g.fill(x, y - 1, x + 2, y + eh, ACCENT_CYAN); }
                String original = text, translation = "";
                int split = text.indexOf('\n');
                if (split >= 0) { original = text.substring(0, split); translation = text.substring(split + 1); }
                int color = current ? ACCENT_CYAN : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
                drawLyric(g, original, x + 8, y + 2, ew - 12, color);
                if (!translation.isBlank()) drawLyric(g, translation, x + 8, y + 12, ew - 12, current ? 0xFFB8E0DC : TEXT_DIM);
            }
            private void drawLyric(GuiGraphics g, String value, int x, int y, int width, int color) {
                String display = value;
                if (font.width(display) > width) display = font.plainSubstrByWidth(display, Math.max(1, width - font.width("…"))) + "…";
                g.drawString(font, display, x, y, color, false);
            }
            @Override public @NotNull Component getNarration() { return Component.literal(text); }
        }
    }
}
