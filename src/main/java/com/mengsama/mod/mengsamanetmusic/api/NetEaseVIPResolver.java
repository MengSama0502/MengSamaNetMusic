package com.mengsama.mod.mengsamanetmusic.api;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class NetEaseVIPResolver {
    private final NetEaseApi api;

    public NetEaseVIPResolver(NetEaseApi api) {
        this.api = api;
    }

    public NetEaseVIPResolver(String cookie) {
        this.api = new NetEaseApi();
        this.api.setCookie(cookie);
    }

    public CompletableFuture<SongInfo> resolveVipSong(long musicId, NetEaseMusicLevel level) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = api.getPlayInfo(musicId, level);

                com.google.gson.JsonObject tree = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
                if (tree.get("code").getAsInt() != 200) {
                    MengSamaNetMusic.LOGGER.error("Failed to get play info for music id: {}", musicId);
                    return null;
                }
                var data = tree.getAsJsonArray("data");
                if (data == null || data.isEmpty()) {
                    return null;
                }
                var playInfo = data.get(0).getAsJsonObject();
                String url = playInfo.has("url") ? playInfo.get("url").getAsString() : "";
                if (url == null || url.isBlank()) {
                    return null;
                }
                SongInfo info = new SongInfo();
                info.songUrl = url;
                info.songTime = playInfo.has("time") ? playInfo.get("time").getAsInt() : 0;
                return info;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to resolve VIP song: {}", musicId, e);
                return null;
            }
        });
    }
}
