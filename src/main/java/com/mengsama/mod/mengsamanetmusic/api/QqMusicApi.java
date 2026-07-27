package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import net.minecraft.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** High-level catalog calls. Playback URL resolution lives in {@link QqMusicUtils}. */
public final class QqMusicApi {
    private QqMusicApi() {}

    public static CompletableFuture<List<SongInfo>> search(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<SongInfo> songs = new ArrayList<>();
                for (QqSearchResult result : QqMusicUtils.search(query)) songs.add(fromSearch(result));
                return List.copyOf(songs);
            } catch (Exception error) {
                MengSamaNetMusic.LOGGER.warn("QQ catalog search failed", error);
                return List.of();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<List<SongInfo>> fetchAlbumSongs(String albumMid) {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("begin", 0);
        parameters.addProperty("num", 500);
        parameters.addProperty("order", 1);
        parameters.addProperty("albumMid", albumMid);
        return catalogList("music.musichallAlbum.AlbumSongList", "GetAlbumSongList", parameters, "songList", true);
    }

    public static CompletableFuture<List<SongInfo>> fetchPlaylistSongs(String playlistId) {
        JsonObject parameters = new JsonObject();
        try { parameters.addProperty("disstid", Long.parseLong(playlistId)); }
        catch (NumberFormatException ignored) { parameters.addProperty("disstid", playlistId); }
        parameters.addProperty("userinfo", 1);
        parameters.addProperty("tag", 1);
        parameters.addProperty("is_pc", 1);
        return catalogList("music.srfDissInfo.aiDissInfo", "uniform_get_Dissinfo", parameters, "songlist", false);
    }

    public static CompletableFuture<SongInfo> fetchSongDetail(String songMid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return QqMusicUtils.fetchTrack(songMid);
            } catch (Exception error) {
                MengSamaNetMusic.LOGGER.debug("QQ track detail unavailable for {}", songMid, error);
                return null;
            }
        }, Util.backgroundExecutor());
    }

    private static CompletableFuture<List<SongInfo>> catalogList(String module, String method,
                                                                  JsonObject parameters, String listKey,
                                                                  boolean entriesWrapSongInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject root = JsonParser.parseString(QqMusicUtils.catalogRequest(module, method, parameters)).getAsJsonObject();
                JsonObject response = root.getAsJsonObject("music");
                JsonObject data = response == null ? null : response.getAsJsonObject("data");
                JsonArray entries = data == null ? null : data.getAsJsonArray(listKey);
                if (entries == null) return List.of();
                List<SongInfo> songs = new ArrayList<>();
                for (JsonElement entry : entries) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject value = entry.getAsJsonObject();
                    if (entriesWrapSongInfo && value.has("songInfo") && value.get("songInfo").isJsonObject()) {
                        value = value.getAsJsonObject("songInfo");
                    }
                    SongInfo song = QqMusicUtils.decodeTrack(value);
                    if (song != null) songs.add(song);
                }
                return List.copyOf(songs);
            } catch (Exception error) {
                MengSamaNetMusic.LOGGER.warn("QQ catalog list request failed: {}.{}", module, method, error);
                return List.of();
            }
        }, Util.backgroundExecutor());
    }

    private static SongInfo fromSearch(QqSearchResult result) {
        SongInfo song = new SongInfo(result.getId(), result.getTitle(), result.getDuration());
        song.providerId = result.getId();
        song.source = "qq";
        song.vip = result.isVip();
        song.albumMid = result.getAlbumMid();
        song.albumName = result.getAlbumName();
        song.coverUrl = result.getCoverUrl();
        song.picUrl = result.getCoverUrl();
        if (!result.getSinger().isBlank()) song.artists.add(result.getSinger());
        return song;
    }
}
