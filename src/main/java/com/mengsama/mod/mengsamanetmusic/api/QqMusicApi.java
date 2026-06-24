package com.mengsama.mod.mengsamanetmusic.api;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class QqMusicApi {
    private static final String API_URL = "https://u6.y.qq.com/cgi-bin/musicu.fcg";

    private QqMusicApi() {
    }

    public static CompletableFuture<List<SongInfo>> search(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                param.addProperty("grp", 1);
                param.addProperty("num_per_page", 50);
                param.addProperty("page_num", 1);
                param.addProperty("query", query);
                param.addProperty("search_type", 0);

                JsonObject req = new JsonObject();
                req.addProperty("method", "DoSearchForQQMusicDesktop");
                req.addProperty("module", "music.search.SearchCgiService");
                req.add("param", param);

                JsonObject comm = new JsonObject();
                comm.addProperty("ct", "19");
                comm.addProperty("cv", "1859");
                comm.addProperty("uin", "0");

                JsonObject body = new JsonObject();
                body.add("comm", comm);
                body.add("req", req);

                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();
                if (getIntOrDefault(tree, "code", -1) != 0) {
                    return Collections.emptyList();
                }
                JsonArray list = tree.getAsJsonObject("req")
                        .getAsJsonObject("data")
                        .getAsJsonObject("body")
                        .getAsJsonObject("song")
                        .getAsJsonArray("list");

                List<SongInfo> results = new ArrayList<>();
                for (JsonElement songElement : list) {
                    JsonObject song = songElement.getAsJsonObject();
                    SongInfo info = new SongInfo();
                    info.songUrl = song.get("mid").getAsString();
                    info.songName = song.get("name").getAsString();
                    info.songTime = song.has("interval") ? song.get("interval").getAsInt() : 0;
                    info.vip = song.getAsJsonObject("pay").get("pay_play").getAsInt() == 1;
                    JsonArray singers = song.getAsJsonArray("singer");
                    for (JsonElement s : singers) {
                        info.artists.add(s.getAsJsonObject().get("name").getAsString());
                    }
                    results.add(info);
                }
                return results;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to search QQ music", e);
                return Collections.emptyList();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<List<SongInfo>> fetchAlbumSongs(String albumMid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                param.addProperty("begin", 0);
                param.addProperty("num", 500);
                param.addProperty("order", 1);
                param.addProperty("albumMid", albumMid);

                JsonObject body = buildRequest("GetAlbumSongList", "music.musichallAlbum.AlbumSongList", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray songList = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("songList");

                return parseSongList(songList, true);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to fetch album songs", e);
                return Collections.emptyList();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<List<SongInfo>> fetchPlaylistSongs(String playlistId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                try {
                    param.addProperty("disstid", Long.parseLong(playlistId));
                } catch (NumberFormatException e) {
                    param.addProperty("disstid", playlistId);
                }
                param.addProperty("userinfo", 1);
                param.addProperty("tag", 1);
                param.addProperty("is_pc", 1);

                JsonObject body = buildRequest("uniform_get_Dissinfo", "music.srfDissInfo.aiDissInfo", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray songList = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("songlist");

                return parseSongList(songList, false);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to fetch playlist songs", e);
                return Collections.emptyList();
            }
        }, Util.backgroundExecutor());
    }

    public static CompletableFuture<SongInfo> fetchSongDetail(String songMid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject param = new JsonObject();
                JsonArray mids = new JsonArray();
                mids.add(songMid);
                param.add("mids", mids);
                param.add("ids", new JsonArray());
                JsonArray types = new JsonArray();
                types.add(0);
                param.add("types", types);

                JsonObject body = buildRequest("GetTrackInfo", "music.trackInfo.UniformRuleCtrl", param);
                String response = postJson(API_URL, body.toString());
                JsonObject tree = JsonParser.parseString(response).getAsJsonObject();

                JsonArray tracks = tree.getAsJsonObject("music")
                        .getAsJsonObject("data")
                        .getAsJsonArray("tracks");

                if (tracks == null || tracks.isEmpty()) {
                    return null;
                }

                return parseSongInfo(tracks.get(0).getAsJsonObject());
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to fetch song detail", e);
                return null;
            }
        }, Util.backgroundExecutor());
    }

    private static JsonObject buildRequest(String method, String module, JsonObject param) {
        JsonObject comm = new JsonObject();
        comm.addProperty("ct", "19");
        comm.addProperty("cv", "2121");

        JsonObject music = new JsonObject();
        music.addProperty("method", method);
        music.addProperty("module", module);
        music.add("param", param);

        JsonObject body = new JsonObject();
        body.add("comm", comm);
        body.add("music", music);
        return body;
    }

    private static List<SongInfo> parseSongList(JsonArray songArray, boolean wrapped) {
        if (songArray == null) {
            return Collections.emptyList();
        }
        List<SongInfo> result = new ArrayList<>();
        for (JsonElement element : songArray) {
            JsonObject songObj = element.getAsJsonObject();
            if (wrapped && songObj.has("songInfo")) {
                songObj = songObj.getAsJsonObject("songInfo");
            }
            SongInfo info = parseSongInfo(songObj);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    private static SongInfo parseSongInfo(JsonObject song) {
        try {
            SongInfo info = new SongInfo();
            info.songName = song.has("name") ? song.get("name").getAsString() : "";
            info.songTime = song.has("interval") ? song.get("interval").getAsInt() : 0;

            String mid = song.has("mid") ? song.get("mid").getAsString() : "";
            info.songUrl = mid;

            if (song.has("singer")) {
                JsonArray singers = song.getAsJsonArray("singer");
                for (JsonElement s : singers) {
                    String name = s.getAsJsonObject().get("name").getAsString();
                    info.artists.add(name);
                }
            }

            if (song.has("pay")) {
                JsonObject pay = song.getAsJsonObject("pay");
                info.vip = pay.has("pay_play") && pay.get("pay_play").getAsInt() == 1;
            }

            return info;
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.error("Failed to parse song info", e);
            return null;
        }
    }

    private static String postJson(String urlStr, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static int getIntOrDefault(JsonObject object, String key, int defaultValue) {
        if (object == null || !object.has(key)) {
            return defaultValue;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }
}
