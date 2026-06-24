package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseSearchResult;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.PlayerAddSongPacket;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
public class MusicPlayerScreen extends AbstractContainerScreen<MusicPlayerMenu> {
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 418;

    private static final int TAB_SEARCH = 0;
    private static final int TAB_PLAYLIST = 1;
    private static final int TAB_LYRICS = 2;
    private int currentTab = TAB_SEARCH;

    private static final int BG_DEEPEST = 0xF60A0A12;
    private static final int BG_PANEL = 0xCC181830;
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
    private Button searchButton;
    private Button playButton;
    private Button stopButton;
    private Button nextButton;
    private Button prevButton;
    private Button modeButton;
    private Button tabSearchBtn;
    private Button tabPlaylistBtn;
    private Button tabLyricsBtn;
    private SearchResultList resultList;
    private PlaylistList playlistList;
    private LyricList lyricList;

    private boolean isSearching;
    private Component statusMessage = Component.empty();
    private int lyricRefreshCounter = 0;

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
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.leftPos;
        int cy = this.topPos;

        int searchBoxW = 196;
        this.searchBox = new EditBox(this.font, cx + 14, cy + SEARCH_Y + 2, searchBoxW, 18,
                Component.translatable("gui.mengsamanetmusic.music_player.search_placeholder"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(50);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.mengsamanetmusic.music_player.search_hint")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        this.addWidget(this.searchBox);

        int searchBtnW = 50;
        this.searchButton = this.addRenderableWidget(Button.builder(
                        Component.literal("\u2315 " + Component.translatable("gui.mengsamanetmusic.music_player.search").getString()),
                        b -> performSearch())
                .pos(cx + 14 + searchBoxW + 6, cy + SEARCH_Y).size(searchBtnW, SEARCH_H).build());

        int tabW = (GUI_WIDTH - 28 - 8) / 3;
        this.tabSearchBtn = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_search"),
                        b -> switchTab(TAB_SEARCH))
                .pos(cx + 14, cy + TAB_Y).size(tabW, TAB_H).build());
        this.tabPlaylistBtn = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_playlist"),
                        b -> switchTab(TAB_PLAYLIST))
                .pos(cx + 14 + tabW + 4, cy + TAB_Y).size(tabW, TAB_H).build());
        this.tabLyricsBtn = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.mengsamanetmusic.music_player.tab_lyrics"),
                        b -> switchTab(TAB_LYRICS))
                .pos(cx + 14 + (tabW + 4) * 2, cy + TAB_Y).size(tabW, TAB_H).build());

        this.resultList = new SearchResultList(this.minecraft, GUI_WIDTH - 28, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, 24);
        this.resultList.setLeftPos(cx + 14);
        this.addWidget(this.resultList);

        this.playlistList = new PlaylistList(this.minecraft, GUI_WIDTH - 28, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, 22);
        this.playlistList.setLeftPos(cx + 14);
        this.addWidget(this.playlistList);

        this.lyricList = new LyricList(this.minecraft, GUI_WIDTH - 28, CONTENT_H,
                cy + CONTENT_Y, cy + CONTENT_Y + CONTENT_H, 16);
        this.lyricList.setLeftPos(cx + 14);
        this.addWidget(this.lyricList);

        int btnY = cy + CONTROLS_Y;
        int btnW = 48;
        int btnH = CONTROLS_H;
        int btnGap = 4;
        int totalBtnW = btnW * 5 + btnGap * 4;
        int btnStartX = cx + (GUI_WIDTH - totalBtnW) / 2;

        this.prevButton = this.addRenderableWidget(Button.builder(
                        Component.literal("\u25C0\u25C0"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_PREV))
                .pos(btnStartX, btnY).size(btnW, btnH).build());
        this.playButton = this.addRenderableWidget(Button.builder(
                        getPlayButtonText(), b -> handleButtonClick(MusicPlayerMenu.BUTTON_PLAY))
                .pos(btnStartX + (btnW + btnGap), btnY).size(btnW, btnH).build());
        this.stopButton = this.addRenderableWidget(Button.builder(
                        Component.literal("\u25A0"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_STOP))
                .pos(btnStartX + (btnW + btnGap) * 2, btnY).size(btnW, btnH).build());
        this.nextButton = this.addRenderableWidget(Button.builder(
                        Component.literal("\u25B6\u25B6"), b -> handleButtonClick(MusicPlayerMenu.BUTTON_NEXT))
                .pos(btnStartX + (btnW + btnGap) * 3, btnY).size(btnW, btnH).build());
        this.modeButton = this.addRenderableWidget(Button.builder(
                        getModeButtonText(this.menu.getPlayMode()), b -> handleButtonClick(MusicPlayerMenu.BUTTON_MODE))
                .pos(btnStartX + (btnW + btnGap) * 4, btnY).size(btnW, btnH).build());

        switchTab(TAB_SEARCH);
    }

    private void switchTab(int tab) {
        this.currentTab = tab;

        if (this.playlistList != null && tab == TAB_PLAYLIST) {
            this.playlistList.refresh();
        }
        if (this.lyricList != null && tab == TAB_LYRICS) {
            this.lyricList.refresh();
        }
    }

    private void handleButtonClick(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private boolean isPlaying() {
        return this.menu.isPlaying();
    }

    private void performSearch() {
        if (this.searchBox == null) return;
        String query = this.searchBox.getValue().trim();
        if (query.isEmpty()) {
            this.statusMessage = Component.translatable("gui.mengsamanetmusic.music_player.search_empty");
            return;
        }
        if (this.isSearching) return;
        this.isSearching = true;
        this.statusMessage = Component.translatable("gui.mengsamanetmusic.cd_burner.searching");
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        String json = MengSamaNetMusic.NET_EASE_API.search(query, 1, 30);
                        return parseSearchResults(json);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, Util.backgroundExecutor())
                .whenComplete((results, error) -> Minecraft.getInstance().execute(() -> {
                    this.isSearching = false;
                    if (error != null) {
                        this.statusMessage = Component.translatable("gui.mengsamanetmusic.cd_burner.search_failed");
                        if (this.resultList != null) this.resultList.setResults(Collections.emptyList());
                    } else {
                        if (this.resultList != null) this.resultList.setResults(results);
                        if (results == null || results.isEmpty()) {
                            this.statusMessage = Component.translatable("gui.mengsamanetmusic.cd_burner.no_result");
                        } else {
                            this.statusMessage = Component.empty();
                        }
                    }
                }));
    }

    private List<NetEaseSearchResult> parseSearchResults(String json) {
        List<NetEaseSearchResult> results = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("result")) return results;
            JsonObject result = root.getAsJsonObject("result");
            if (!result.has("songs")) return results;
            JsonArray songs = result.getAsJsonArray("songs");
            for (int i = 0; i < songs.size(); i++) {
                JsonObject song = songs.get(i).getAsJsonObject();
                long id = song.get("id").getAsLong();
                String name = song.get("name").getAsString();
                String artist = "";
                if (song.has("ar") && song.get("ar").isJsonArray()) {
                    JsonArray ar = song.getAsJsonArray("ar");
                    if (ar.size() > 0) {
                        artist = ar.get(0).getAsJsonObject().get("name").getAsString();
                    }
                }
                int fee = song.has("fee") ? song.get("fee").getAsInt() : 0;
                results.add(new NetEaseSearchResult(String.valueOf(id), name, artist, fee > 0));
            }
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.error("Failed to parse search results", e);
        }
        return results;
    }

    private void onSearchResultClicked(NetEaseSearchResult result, boolean playNow) {
        if (result == null) return;
        this.statusMessage = Component.translatable("gui.mengsamanetmusic.cd_burner.loading");
        CompletableFuture.supplyAsync(() -> {
            try {
                return MengSamaNetMusic.NET_EASE_API.get163Song(Long.parseLong(result.getSongId()));
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to fetch song info", e);
                return null;
            }
        }).thenAccept(song -> Minecraft.getInstance().execute(() -> {
            if (song != null && song.isValid()) {
                ModNetwork.CHANNEL.sendToServer(new PlayerAddSongPacket(song, playNow));
                this.statusMessage = playNow
                        ? Component.literal("\u25B6 " + song.songName).withStyle(ChatFormatting.GREEN)
                        : Component.literal("\u2713 " + song.songName).withStyle(ChatFormatting.AQUA);
            } else {
                this.statusMessage = Component.translatable("gui.mengsamanetmusic.cd_burner.get_info_error");
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

    private SongInfo getSongInfoFromCd(ItemStack cd) {
        if (cd.getItem() instanceof MusicListItem) {
            return MusicListItem.getSongInfo(cd);
        } else if (cd.getItem() instanceof MusicCDItem) {
            return MusicCDItem.getSongInfo(cd);
        }
        return null;
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

        graphics.fill(x, y, x + w, y + h, BG_DEEPEST);
        for (int i = 0; i < h; i++) {
            float ratio = (float) i / h;
            int r = (int) (0x12 + 0x08 * Math.sin(ratio * Math.PI));
            int g = (int) (0x12 + 0x08 * Math.sin(ratio * Math.PI));
            int b = (int) (0x30 + 0x10 * Math.sin(ratio * Math.PI));
            graphics.fill(x, y + i, x + w, y + i + 1, (0xE6 << 24) | (r << 16) | (g << 8) | b);
        }

        for (int i = 0; i < 24; i++) {
            int alpha = (int) (0x40 * (1 - i / 24.0));
            int mid = x + w / 2;
            graphics.fill(x, y + i, mid, y + i + 1, (alpha << 24) | 0x7C6FFF);
            graphics.fill(mid, y + i, x + w, y + i + 1, (alpha << 24) | 0x4ECDC4);
        }

        graphics.fill(x, y, x + w, y + TITLE_H, BG_PANEL);
        for (int i = 0; i < w; i++) {
            float ratio = (float) i / w;
            int r = (int) (0x7C + (0x4E - 0x7C) * ratio);
            int g = (int) (0x6F + (0xCD - 0x6F) * ratio);
            int b = (int) (0xFF + (0xC4 - 0xFF) * ratio);
            graphics.fill(x + i, y + TITLE_H - 1, x + i + 1, y + TITLE_H, (0xFF << 24) | (r << 16) | (g << 8) | b);
        }
        graphics.fill(x, y + TITLE_H, x + w, y + TITLE_H + 2, 0x447C6FFF);

        graphics.fill(x + 8, y + TITLE_H + 2, x + w - 8, y + SEARCH_Y + SEARCH_H + 4, BG_PANEL);
        int searchBoxW = 196;
        graphics.fill(x + 12, y + SEARCH_Y, x + 12 + searchBoxW + 4, y + SEARCH_Y + SEARCH_H, SLOT_BG);
        graphics.renderOutline(x + 12, y + SEARCH_Y, searchBoxW + 4, SEARCH_H, BORDER);
        if (this.searchBox != null && this.searchBox.isFocused()) {
            graphics.renderOutline(x + 12, y + SEARCH_Y, searchBoxW + 4, SEARCH_H, ACCENT);
        }

        graphics.fill(x + 8, y + TAB_Y - 2, x + w - 8, y + TAB_Y + TAB_H + 2, BG_PANEL);

        graphics.fill(x + 8, y + CONTENT_Y - 2, x + w - 8, y + CONTENT_Y + CONTENT_H + 2, BG_PANEL);
        graphics.renderOutline(x + 8, y + CONTENT_Y - 2, w - 16, CONTENT_H + 4, BORDER);
        graphics.fill(x + 9, y + CONTENT_Y - 1, x + w - 9, y + CONTENT_Y, ACCENT_DIM);

        graphics.fill(x + 8, y + CONTROLS_Y - 4, x + w - 8, y + CONTROLS_Y + CONTROLS_H + 4, BG_PANEL);
        graphics.renderOutline(x + 8, y + CONTROLS_Y - 4, w - 16, CONTROLS_H + 8, BORDER_LIGHT);

        graphics.fill(x + 8, y + PROGRESS_Y - 2, x + w - 8, y + PROGRESS_Y + 20, BG_PANEL);

        graphics.fill(x + 8, y + NOW_PLAYING_Y, x + w - 8, y + NOW_PLAYING_Y + 24, BG_PANEL);
        int statusColor = isPlaying() ? STATUS_PLAYING : STATUS_STOPPED;
        graphics.fill(x + 8, y + NOW_PLAYING_Y, x + 10, y + NOW_PLAYING_Y + 24, statusColor);

        int sepY = y + MusicPlayerMenu.INV_Y - 10;
        for (int i = 0; i < w - 16; i++) {
            float ratio = (float) i / (w - 16);
            int alpha = (int) (0x66 * (1 - Math.abs(ratio - 0.5) * 2));
            graphics.fill(x + 8 + i, sepY, x + 8 + i + 1, sepY + 1, (alpha << 24) | 0x7C6FFF);
        }

        int invLeft = x + MusicPlayerMenu.INV_X - 1;
        int invTop = y + MusicPlayerMenu.INV_Y - 1;
        int invWidth = 9 * 18 + 2;
        int invHeight = 3 * 18 + 2;
        graphics.fill(invLeft, invTop, invLeft + invWidth, invTop + invHeight, BG_PANEL_LIGHT);
        graphics.renderOutline(invLeft, invTop, invWidth, invHeight, BORDER_LIGHT);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                graphics.renderOutline(invLeft + 1 + col * 18, invTop + 1 + row * 18, 18, 18, BORDER);
            }
        }

        int hotbarTop = y + MusicPlayerMenu.HOTBAR_Y - 1;
        graphics.fill(invLeft, hotbarTop, invLeft + invWidth, hotbarTop + 20, BG_PANEL_LIGHT);
        graphics.renderOutline(invLeft, hotbarTop, invWidth, 20, BORDER_LIGHT);
        for (int col = 0; col < 9; col++) {
            graphics.renderOutline(invLeft + 1 + col * 18, hotbarTop + 1, 18, 18, BORDER);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.playButton != null) this.playButton.setMessage(getPlayButtonText());
        if (this.modeButton != null) this.modeButton.setMessage(getModeButtonText(this.menu.getPlayMode()));

        super.render(graphics, mouseX, mouseY, partialTicks);

        int x = this.leftPos;
        int y = this.topPos;

        graphics.drawString(font, Component.literal("\u266A").withStyle(ChatFormatting.LIGHT_PURPLE),
                x + 10, y + 7, TEXT_ACCENT, false);
        graphics.drawString(font, this.title, x + 22, y + 7, TEXT_ACCENT, false);

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
        SongInfo hudInfo = MusicInfoHud.getInfo();
        if (hudInfo != null && hudInfo.songTime > 0 && tick >= 0) {
            float progress = Math.max(0, Math.min(1, (tick / 20.0f) / hudInfo.songTime));
            int fillW = (int) (barW * progress);
            if (fillW > 0) {
                for (int i = 0; i < fillW; i++) {
                    float ratio = (float) i / Math.max(1, fillW);
                    int r = (int) (0x7C + (0x4E - 0x7C) * ratio);
                    int g = (int) (0x6F + (0xCD - 0x6F) * ratio);
                    int b = (int) (0xFF + (0xC4 - 0xFF) * ratio);
                    graphics.fill(barX + i, barY, barX + i + 1, barY + barH, (0xFF << 24) | (r << 16) | (g << 8) | b);
                }
                graphics.fill(barX, barY, barX + fillW, barY + 1, 0x66FFFFFF);
                int handleX = barX + fillW - 2;
                graphics.fill(handleX, barY - 1, handleX + 4, barY + barH + 1, ACCENT_BRIGHT);
            }
            String currentTime = formatTime(tick / 20);
            String totalTime = formatTime(hudInfo.songTime);
            graphics.drawString(font, currentTime, barX, barY + barH + 3, TEXT_DIM, false);
            int totalW = font.width(totalTime);
            graphics.drawString(font, totalTime, barX + barW - totalW, barY + barH + 3, TEXT_DIM, false);
        }
    }

    private void renderNowPlaying(GuiGraphics graphics, int x, int y) {
        SongInfo hudInfo = MusicInfoHud.getInfo();
        int infoY = y + NOW_PLAYING_Y + 4;
        boolean playing = isPlaying();

        String statusIcon = playing ? "\u25B6" : "\u25A0";
        int statusColor = playing ? STATUS_PLAYING : STATUS_STOPPED;
        graphics.drawString(font, statusIcon, x + 14, infoY, statusColor, false);
        Component statusText = playing
                ? Component.translatable("gui.mengsamanetmusic.music_player.playing")
                : Component.translatable("gui.mengsamanetmusic.music_player.stopped");
        graphics.drawString(font, statusText, x + 26, infoY, statusColor, false);

        if (hudInfo != null && hudInfo.songName != null) {
            String name = hudInfo.songName;
            int maxW = this.imageWidth - 80;
            if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - 8) + "...";
            graphics.drawString(font, name, x + 14, infoY + 12, TEXT_PRIMARY, false);

            String sourceName = SongInfo.getSourceDisplayName(hudInfo.source);
            int sourceColor = SongInfo.getSourceColor(hudInfo.source);
            int sourceX = x + 14 + font.width(name) + 6;
            int sourceW = font.width(sourceName) + 4;
            if (sourceX + sourceW < x + this.imageWidth - 14) {
                graphics.fill(sourceX, infoY + 11, sourceX + sourceW, infoY + 21, 0x55000000);
                graphics.drawString(font, sourceName, sourceX + 2, infoY + 12, sourceColor, false);
            }
        }
    }

    private int getPlayingTick() {
        try {
            if (com.mengsama.mod.mengsamanetmusic.util.NetMusicSound.currentTick >= 0)
                return com.mengsama.mod.mengsamanetmusic.util.NetMusicSound.currentTick;
            if (com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound.currentTick >= 0)
                return com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound.currentTick;
        } catch (Exception ignored) {}
        return -1;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.searchBox != null) this.searchBox.tick();
        if (currentTab == TAB_LYRICS && this.lyricList != null) {
            lyricRefreshCounter++;
            if (lyricRefreshCounter >= 10) {
                lyricRefreshCounter = 0;
                this.lyricList.refresh();
            }
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

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final NetEaseSearchResult result;
            public Entry(NetEaseSearchResult r) { this.result = r; }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                               int mx, int my, boolean hovered, float pt) {
                int bg = hovered ? BG_CARD_HOVER : BG_CARD;
                g.fill(x, y, x + ew, y + eh - 2, bg);
                if (hovered) g.fill(x, y, x + 2, y + eh - 2, ACCENT);
                g.fill(x, y, x + ew, y + 1, 0x22FFFFFF);

                int textY = y + 4;
                int pos = x + 6;

                if (result.isVip()) {
                    g.fill(pos - 1, textY - 1, pos + 24, textY + 9, 0x55FF4444);
                    pos = g.drawString(font, Component.literal("[VIP]")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), pos, textY, 0xFFFF4444, false) + 4;
                }

                String name = result.getSongName();
                int maxW = ew - (pos - x) - 10;
                if (font.width(name) > maxW) name = font.plainSubstrByWidth(name, maxW - 8) + "...";
                g.drawString(font, name, pos, textY, hovered ? 0xFFFFFFFF : 0xFFE0E0E0, false);

                String artist = result.getArtistName();
                if (artist != null && !artist.isEmpty()) {
                    int artistMaxW = ew - 12;
                    if (font.width(artist) > artistMaxW) artist = font.plainSubstrByWidth(artist, artistMaxW - 8) + "...";
                    g.drawString(font, artist, x + 6, textY + 10, TEXT_SECONDARY, false);
                }
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button == 0) { onSearchResultClicked(result, true); return true; }
                if (button == 1) { onSearchResultClicked(result, false); return true; }
                return false;
            }

            @Override
            public @NotNull Component getNarration() { return Component.literal(result.getDisplayText()); }
        }
    }

    private class PlaylistList extends ObjectSelectionList<PlaylistList.Entry> {
        private List<SongInfo> songs = new ArrayList<>();
        private List<Integer> slotIndices = new ArrayList<>();

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
            songs.clear();
            slotIndices.clear();
            if (minecraft != null && minecraft.player != null) {
                ItemStack item = MusicPlayerItem.findMusicPlayerItem(minecraft.player);
                if (!item.isEmpty()) {
                    var cds = MusicPlayerItem.loadAllCds(item);
                    for (int i = 0; i < cds.size(); i++) {
                        if (!cds.get(i).isEmpty()) {
                            SongInfo info = getSongInfoFromCd(cds.get(i));
                            if (info != null) {
                                songs.add(info);
                                slotIndices.add(i);
                            }
                        }
                    }
                }
            }
            this.clearEntries();
            for (int i = 0; i < songs.size(); i++) {
                this.addEntry(new Entry(songs.get(i), i));
            }
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final SongInfo song;
            private final int displayIndex;

            public Entry(SongInfo song, int displayIndex) {
                this.song = song;
                this.displayIndex = displayIndex;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                               int mx, int my, boolean hovered, float pt) {
                int currentPlayIndex = menu.getPlayIndex();
                boolean isCurrent = !slotIndices.isEmpty() && displayIndex < slotIndices.size()
                        && slotIndices.get(displayIndex) == currentPlayIndex;

                int bg = isCurrent ? 0x664ECDC4 : (hovered ? BG_CARD_HOVER : BG_CARD);
                g.fill(x, y, x + ew, y + eh - 2, bg);
                int leftColor = isCurrent ? ACCENT_CYAN : (hovered ? ACCENT : 0x337C6FFF);
                g.fill(x, y, x + 2, y + eh - 2, leftColor);
                g.fill(x, y, x + ew, y + 1, 0x22FFFFFF);

                int textY = y + 4;

                String numStr = (isCurrent ? "\u25B6 " : "") + (displayIndex + 1) + ".";
                g.drawString(font, numStr, x + 6, textY, isCurrent ? ACCENT_CYAN : TEXT_DIM, false);

                String name = song.songName != null ? song.songName : "Unknown";
                int nameColor = isCurrent ? TEXT_PRIMARY : (hovered ? 0xFFFFFFFF : 0xFFE0E0E0);
                int nameW = ew - 50;
                if (font.width(name) > nameW) name = font.plainSubstrByWidth(name, nameW - 8) + "...";
                g.drawString(font, name, x + 36, textY, nameColor, false);

                if (song.artists != null && !song.artists.isEmpty()) {
                    String artist = String.join(", ", song.artists);
                    int artistW = ew - 12;
                    if (font.width(artist) > artistW) artist = font.plainSubstrByWidth(artist, artistW - 8) + "...";
                    g.drawString(font, artist, x + 6, textY + 11, TEXT_SECONDARY, false);
                }

                if (song.songTime > 0) {
                    String time = formatTime(song.songTime);
                    int timeW = font.width(time);
                    g.drawString(font, time, x + ew - timeW - 6, textY, TEXT_DIM, false);
                }
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (displayIndex < slotIndices.size()) {
                    if (button == 0) {
                        handleButtonClick(MusicPlayerMenu.BUTTON_SELECT_BASE + slotIndices.get(displayIndex));
                        return true;
                    } else if (button == 1) {
                        handleButtonClick(MusicPlayerMenu.BUTTON_DELETE_BASE + slotIndices.get(displayIndex));
                        return true;
                    }
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
        private List<Map.Entry<Float, String>> lyricLines = new ArrayList<>();
        private List<Map.Entry<Float, String>> transformLines = new ArrayList<>();
        private int currentLine = -1;

        public LyricList(Minecraft mc, int w, int h, int top, int bottom, int itemH) {
            super(mc, w, h, top, bottom, itemH);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        @Override
        public int getRowWidth() { return this.getWidth() - 16; }

        @Override
        protected int getScrollbarPosition() { return this.getLeft() + this.getWidth() - 6; }

        public void refresh() {
            NetMusicListUtil.Lyric lyric = MusicInfoHud.getLyric();
            lyricLines.clear();
            transformLines.clear();
            if (lyric != null && lyric.getLyric() != null) {
                lyricLines.addAll(lyric.getLyric().entrySet());
            }
            if (lyric != null && lyric.getTransformLyric() != null) {
                transformLines.addAll(lyric.getTransformLyric().entrySet());
            }

            int tick = getPlayingTick();
            float currentSecond = tick >= 0 ? tick / 20.0f : 0;
            currentLine = -1;
            for (int i = 0; i < lyricLines.size(); i++) {
                if (lyricLines.get(i).getKey() <= currentSecond) {
                    currentLine = i;
                } else {
                    break;
                }
            }

            this.clearEntries();
            for (int i = 0; i < lyricLines.size(); i++) {
                String original = lyricLines.get(i).getValue();
                String translation = "";
                if (i < transformLines.size()) {
                    translation = transformLines.get(i).getValue();
                }
                this.addEntry(new Entry(original, translation, i, i == currentLine));
            }

            if (currentLine >= 0 && this.getItemCount() > 0) {
                this.centerScrollOn(this.getEntry(currentLine));
            }
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String text;
            private final String translation;
            private final int lineIndex;
            private final boolean isCurrent;

            public Entry(String text, String translation, int lineIndex, boolean isCurrent) {
                this.text = text != null ? text : "";
                this.translation = translation != null ? translation : "";
                this.lineIndex = lineIndex;
                this.isCurrent = isCurrent;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int ew, int eh,
                               int mx, int my, boolean hovered, float pt) {
                if (isCurrent) {
                    g.fill(x, y - 1, x + ew, y + eh, 0x444ECDC4);
                    g.fill(x, y - 1, x + 2, y + eh, ACCENT_CYAN);
                }

                int textColor = isCurrent ? ACCENT_CYAN : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
                int textY = y + 2;

                if (!text.isEmpty()) {
                    String display = text;
                    int maxW = ew - 8;
                    if (font.width(display) > maxW) display = font.plainSubstrByWidth(display, maxW - 8) + "...";
                    g.drawString(font, display, x + 8, textY, textColor, false);
                }

                if (!translation.isEmpty()) {
                    String display = translation;
                    int maxW = ew - 8;
                    if (font.width(display) > maxW) display = font.plainSubstrByWidth(display, maxW - 8) + "...";
                    g.drawString(font, display, x + 8, textY + 10, isCurrent ? 0xFFB8E0DC : TEXT_DIM, false);
                }
            }

            @Override
            public @NotNull Component getNarration() { return Component.literal(text); }
        }
    }
}
