package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.api.NetEaseSearchResult;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.client.SongCoverCache;
import com.mengsama.mod.mengsamanetmusic.config.MusicPlayerUiConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class SongRowRenderer {
    static final int ACTION_WIDTH = 40;
    static final int ACTION_GAP = 4;

    enum Action { NONE, ADD, DELETE }

    private SongRowRenderer() {}

    static void renderSearch(GuiGraphics g, Font font, NetEaseSearchResult song,
                             int x, int y, int w, int h, boolean hovered, int bg, int hoverBg) {
        renderCard(g, x, y, w, h, hovered, bg, hoverBg, song.getCoverUrl());
        renderColumns(g, font, song.getSongName(), song.getArtistName(), song.getAlbumName(),
                x, y, contentWidth(w), h, false, 0xFFFFFFFF);
        renderAction(g, font, x, y, w, h, Action.ADD, hovered && hitAction(x + w - ACTION_WIDTH + 1, y + h / 2, x, y, w, h));
    }

    static void renderPlaylist(GuiGraphics g, Font font, SongInfo song, int realSlot,
                               int x, int y, int w, int h, boolean hovered, boolean current, int bg, int hoverBg) {
        int themedSelection = (0x66 << 24) | MusicPlayerUiConfig.get().themeRgb();
        renderCard(g, x, y, w, h, hovered, current ? themedSelection : bg, hoverBg, song.preferredCoverUrl());
        String prefix = (current ? "▶ " : "") + (realSlot + 1) + ". ";
        String artist = song.artists == null || song.artists.isEmpty() ? "未知歌手" : String.join(", ", song.artists);
        renderColumns(g, font, prefix + SongRowLayout.value(song.songName, "未知歌曲"), artist,
                SongRowLayout.value(song.albumName, "未知专辑"), x, y, contentWidth(w), h, current,
                current ? MusicPlayerUiConfig.get().accent() : 0xFFFFFFFF);
        renderAction(g, font, x, y, w, h, Action.DELETE, hovered && hitAction(x + w - ACTION_WIDTH + 1, y + h / 2, x, y, w, h));
    }

    static boolean hitAction(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x + w - ACTION_WIDTH && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    private static int contentWidth(int rowWidth) {
        return Math.max(0, rowWidth - ACTION_WIDTH - ACTION_GAP);
    }

    private static void renderAction(GuiGraphics g, Font font, int x, int y, int w, int h,
                                     Action action, boolean hovered) {
        int bx = x + w - ACTION_WIDTH;
        int by = y + Math.max(1, (h - 16) / 2);
        int color = hovered ? 0xFF4ECDC4 : 0xFF30304A;
        g.fill(bx, by, bx + ACTION_WIDTH, by + 16, color);
        g.renderOutline(bx, by, ACTION_WIDTH, 16, hovered ? 0xFF9D8AFF : 0xFF55556E);
        String key = action == Action.ADD ? "gui.mengsamanetmusic.music_player.add" : "gui.mengsamanetmusic.music_player.delete";
        String text = net.minecraft.network.chat.Component.translatable(key).getString();
        int tw = font.width(text);
        g.drawString(font, text, bx + Math.max(2, (ACTION_WIDTH - tw) / 2), by + 4, 0xFFFFFFFF, false);
    }

    static void renderLegacy(GuiGraphics g, Font font, SongInfo song,
                             int x, int y, int w, int h, boolean current) {
        String prefix = current ? "▶ " : "";
        String artist = song.artists == null || song.artists.isEmpty() ? "未知歌手" : String.join(", ", song.artists);
        renderTextColumns(g, font, prefix + SongRowLayout.value(song.songName, "未知歌曲"), artist,
                SongRowLayout.value(song.albumName, "未知专辑"), x, y, w, h,
                current ? MusicPlayerUiConfig.get().accent() : 0xFFFFFFFF);
    }

    private static void renderColumns(GuiGraphics g, Font font, String title, String artist, String album,
                                      int x, int y, int w, int h, boolean current, int titleColor) {
        int cover = Math.max(0, Math.min(20, h - 4));
        int tx = x + cover + 6;
        renderTextColumns(g, font, title, artist, album, tx, y, Math.max(0, w - (tx - x) - 6), h, titleColor);
    }

    private static void renderTextColumns(GuiGraphics g, Font font, String title, String artist, String album,
                                          int x, int y, int w, int h, int titleColor) {
        title = SongRowLayout.value(title, "未知歌曲");
        artist = SongRowLayout.value(artist, "未知歌手");
        album = SongRowLayout.value(album, "未知专辑");
        SongRowLayout.Columns columns = SongRowLayout.allocate(w, font.width(title), font.width(artist), font.width(album));
        int ty = y + Math.max(0, (h - font.lineHeight - 2) / 2);
        drawClipped(g, font, title, x, ty, columns.title(), titleColor);
        int artistX = x + columns.title() + columns.gap();
        drawClipped(g, font, artist, artistX, ty, columns.artist(), 0xFFB8B8CC);
        int albumX = artistX + columns.artist() + columns.gap();
        drawClipped(g, font, album, albumX, ty, columns.album(), 0xFF8A8A9E);
    }

    private static void renderCard(GuiGraphics g, int x, int y, int w, int h, boolean hovered,
                                   int bg, int hoverBg, String coverUrl) {
        g.fill(x, y, x + w, y + h - 2, hovered ? hoverBg : bg);
        int size = Math.max(0, Math.min(20, h - 4));
        int cx = x + 2, cy = y + Math.max(1, (h - size - 2) / 2);
        if (size <= 0) return;
        g.fill(cx, cy, cx + size, cy + size, 0xFF29293D);
        g.renderOutline(cx, cy, size, size, 0xFF44445E);
        ResourceLocation texture = SongCoverCache.getOrRequest(coverUrl);
        if (texture != null) g.blit(texture, cx, cy, 0, 0, size, size, size, size);
        else {
            int inset = Math.max(2, size / 5);
            g.fill(cx + inset, cy + inset, cx + size - inset, cy + size - inset, 0x553FBDD1);
            g.fill(cx + size / 2 - 1, cy + inset, cx + size / 2 + 1, cy + size - inset, 0xFF9D8AFF);
        }
    }

    static String formatDuration(int seconds) {
        if (seconds <= 0) return "--:--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static void drawClipped(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        String value = text == null ? "" : text;
        if (font.width(value) > maxWidth) {
            int ellipsis = font.width("…");
            value = ellipsis > maxWidth ? "" : font.plainSubstrByWidth(value, Math.max(0, maxWidth - ellipsis)) + "…";
        }
        if (!value.isEmpty()) g.drawString(font, value, x, y, color, false);
    }
}
