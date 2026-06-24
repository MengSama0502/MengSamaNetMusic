package com.mengsama.mod.mengsamanetmusic.util;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.ExtraMusicList;
import com.mengsama.mod.mengsamanetmusic.api.LyricRecord;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseMusicList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import oshi.util.tuples.Pair;

import org.jetbrains.annotations.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetMusicListUtil {
    public static final Gson GSON = new Gson();
    public static boolean globalStopMusic = false;

    @OnlyIn(Dist.CLIENT)
    public static void playSound(SoundEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1));
    }

    @OnlyIn(Dist.CLIENT)
    public static long getIdFromInfo(SongInfo info) throws IllegalAccessException {
        var s = info.songUrl;
        return getIdFromUrl(s);
    }

    public static long getIdFromUrl(String url) throws IllegalAccessException {
        String[] parts = url.split("[?&]id=");
        String idPart;
        if (parts.length > 1) {
            idPart = parts[1].split("&")[0];
        } else {
            throw new IllegalAccessException("\u89E3\u6790\u5931\u8D25");
        }
        return Long.parseLong(idPart.replace(".mp3", ""));
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("all")
    public static URL getIconUrl(String json) throws Exception {
        var data = (Map<String, Object>) GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        var song = (Map<String, Object>) ((List<Object>) data.get("songs")).get(0);
        var album = song.get("album");
        return new URL((String) ((Map<String, Object>) album).get("picUrl"));
    }

    @OnlyIn(Dist.CLIENT)
    public static AbstractTexture getTextureFromURL(URL imageUrl) throws IOException {
        try (InputStream stream = imageUrl.openConnection().getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
                throw new IOException("\u65E0\u6CD5\u8BFB\u53D6\u56FE\u7247 - \u4E0D\u652F\u6301\u7684\u683C\u5F0F\u6216\u635F\u574F\u7684\u6587\u4EF6");
            }
            NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int argb = bufferedImage.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int rgba = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setPixelRGBA(x, y, rgba | 0xFF000000);
                }
            }
            return new DynamicTexture(nativeImage);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static AbstractTexture getTextureFromPath(Path imagePath) throws IOException {
        try (InputStream stream = Files.newInputStream(imagePath)) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage == null) {
                throw new IOException("\u65E0\u6CD5\u8BFB\u53D6\u56FE\u7247 - \u4E0D\u652F\u6301\u7684\u683C\u5F0F\u6216\u635F\u574F\u7684\u6587\u4EF6");
            }
            NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    int argb = bufferedImage.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int rgba = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setPixelRGBA(x, y, rgba | 0xFF000000);
                }
            }
            return new DynamicTexture(nativeImage);
        }
    }

    public static class Lyric {
        @SerializedName("lyric")
        private final Map<Float, String> lyric;

        @SerializedName("transform_lyric")
        @Nullable
        private final Map<Float, String> transformLyric;

        public Lyric(Map<Float, String> lyric, @Nullable Map<Float, String> transformLyric) {
            this.lyric = lyric;
            this.transformLyric = transformLyric;
        }

        public Lyric() {
            this.lyric = new TreeMap<>();
            this.transformLyric = null;
        }

        public Pair<String, String> getLyric(float second) {
            var keyList = lyric.keySet().stream().toList();
            var valueList = lyric.values().stream().toList();
            var text = "";
            var lyricTime = 0f;
            String transformText = null;
            for (int i = 0; i < lyric.size(); i++) {
                if (i == lyric.size() - 1) {
                    text = valueList.get(i);
                    lyricTime = keyList.get(i);
                    break;
                }
                if (keyList.get(i) <= second && keyList.get(i + 1) > second) {
                    text = valueList.get(i);
                    lyricTime = keyList.get(i);
                    break;
                }
            }
            if (transformLyric != null) {
                transformText = transformLyric.getOrDefault(lyricTime, null);
            }
            return new Pair<>(text, transformText);
        }

        public Map<Float, String> getLyric() {
            return lyric;
        }

        @Nullable
        public Map<Float, String> getTransformLyric() {
            return transformLyric;
        }

        public String toJson() {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            return gson.toJson(this);
        }

        public static Lyric fromJson(String json) {
            Gson gson = new Gson();
            return gson.fromJson(json, Lyric.class);
        }

        public LyricRecord toLyricRecord() {
            Int2ObjectSortedMap<String> lyric1 = new Int2ObjectRBTreeMap<>();
            Int2ObjectSortedMap<String> lyric2;
            lyric.forEach((k, v) -> lyric1.put((int) (k / 0.05f), v));
            if (transformLyric != null) {
                lyric2 = new Int2ObjectRBTreeMap<>();
                transformLyric.forEach((k, v) -> lyric2.put((int) (k / 0.05f), v));
            } else {
                lyric2 = null;
            }
            return new LyricRecord(lyric1, lyric2);
        }
    }

    public static String secondsToMinutesSeconds(int totalSeconds) {
        if (totalSeconds < 0) {
            return "00:00";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @SuppressWarnings("all")
    public static Lyric getLyric(String json) {
        var data = (Map<String, Object>) GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        var lrc = (String) ((Map<String, Object>) data.get("lrc")).get("lyric");
        var transformlLrc = "";
        if (data.containsKey("tlyric")) {
            transformlLrc = (String) ((Map<String, Object>) data.get("tlyric")).get("lyric");
        }
        if (lrc.isEmpty()) {
            return null;
        }
        Map<Float, String> lyricMap;
        Map<Float, String> transformLyricMap = null;
        if (!transformlLrc.isEmpty()) {
            transformLyricMap = new LinkedHashMap<>();
            for (String part : transformlLrc.split("\n")) {
                var p = getLyricPair(part);
                if (p != null) {
                    transformLyricMap.put(p.getA(), p.getB());
                }
            }
        }
        lyricMap = new LinkedHashMap<>();
        for (String part : lrc.split("\n")) {
            var p = getLyricPair(part);
            if (p != null) {
                lyricMap.put(p.getA(), p.getB());
            }
        }
        return new Lyric(lyricMap, transformLyricMap);
    }

    private static Pair<Float, String> getLyricPair(String input) {
        Pattern pattern = Pattern.compile("^\\[(\\d+):(\\d+)[.:](\\d+)](.*)$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            int milliseconds = Integer.parseInt(matcher.group(3));
            String text = matcher.group(4);

            float totalSeconds = minutes * 60 + seconds + milliseconds / 1000f;
            return new Pair<>(totalSeconds, text.trim());
        }
        return null;
    }

    public static List<TickableSoundInstance> getTickableSounds() {
        return ((com.mengsama.mod.mengsamanetmusic.mixin.TickableSoundGetterMixins.SoundEngineMixin) ((com.mengsama.mod.mengsamanetmusic.mixin.TickableSoundGetterMixins.SoundManagerMixin) Minecraft.getInstance().getSoundManager()).getSoundEngine()).getTickableSoundInstances();
    }

    public static boolean isPaused() {
        return ((com.mengsama.mod.mengsamanetmusic.client.PauseSoundManager) Minecraft.getInstance().getSoundManager()).isPaused();
    }

    public static List<SongInfo> getMusicList(long id) throws Exception {
        var SONGS = new ArrayList<SongInfo>();
        NetEaseMusicList pojo = GSON.fromJson(MengSamaNetMusic.NET_EASE_API.list(id), NetEaseMusicList.class);
        int count = pojo.getPlayList().getTracks().size();
        int size = Math.min(pojo.getPlayList().getTrackIds().size(), 1000);
        if (count < size) {
            long[] ids = new long[size - count];
            for (int i = count; i < size; ++i) {
                ids[i - count] = pojo.getPlayList().getTrackIds().get(i).getId();
            }
            if (ids.length <= 100) {
                String extraTrackInfo = MengSamaNetMusic.NET_EASE_API.songs(ids);
                ExtraMusicList extra = GSON.fromJson(extraTrackInfo, ExtraMusicList.class);
                pojo.getPlayList().getTracks().addAll(extra.getTracks());
            } else {
                int batchSize = 100;
                for (int i = 0; i < ids.length; i += batchSize) {
                    int end = Math.min(i + batchSize, ids.length);
                    long[] batchIds = Arrays.copyOfRange(ids, i, end);
                    String extraTrackInfo = MengSamaNetMusic.NET_EASE_API.songs(batchIds);
                    ExtraMusicList extra = GSON.fromJson(extraTrackInfo, ExtraMusicList.class);
                    pojo.getPlayList().getTracks().addAll(extra.getTracks());
                }
            }
        }
        for (NetEaseMusicList.Track track : pojo.getPlayList().getTracks()) {
            SongInfo info = new SongInfo();
            info.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", track.getId());
            info.songName = track.getName();
            info.songTime = track.getDt() / 1000;
            if (track.getAr() != null) {
                for (var artist : track.getAr()) {
                    info.artists.add(artist.getName());
                }
            }
            info.vip = track.getFee() > 0;
            info.readOnly = false;
            SONGS.add(info);
        }
        return SONGS;
    }

    public static URL resolveRedirect(URL originalUrl, int maxRedirects, Map<String, String> headers) throws IOException {
        URL currentUrl = originalUrl;
        HttpURLConnection connection;

        while (maxRedirects-- > 0) {
            connection = (HttpURLConnection) currentUrl.openConnection();
            connection.setInstanceFollowRedirects(false);

            if (headers != null) {
                headers.forEach(connection::setRequestProperty);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return currentUrl;
            }

            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {

                String location = connection.getHeaderField("Location");
                connection.disconnect();

                if (location == null) {
                    throw new IOException("Redirect with no Location header: " + responseCode);
                }

                currentUrl = new URL(currentUrl, location);
            } else {
                throw new IOException("Unexpected HTTP status: " + responseCode);
            }
        }

        throw new IOException("Too many redirects (max: " + maxRedirects + ")");
    }

    public static String getSongJson(long songId) {
        try {
            return MengSamaNetMusic.NET_EASE_API.song(songId);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getLyricJson(long songId) {
        try {
            return MengSamaNetMusic.NET_EASE_API.lyric(songId);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getVIPUrl(String oldURL) {
        try {
            long id = getIdFromUrl(oldURL);
            return getVIPUrlById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getVIPUrlById(long id) {

        long[] qualityLevels = {320, 192, 128};
        Map<String, String> headers = MengSamaNetMusic.NET_EASE_API.getRequestPropertyData();
        boolean hasCookie = headers.containsKey("Cookie") && !headers.get("Cookie").isBlank();
        if (!hasCookie) {
            MengSamaNetMusic.LOGGER.warn("getVIPUrlById: No Cookie set, VIP song {} may not play", id);
        }

        String[] apiEndpoints = {
                "https://music.163.com/weapi/song/enhance/player/url?csrf_token=",
                "http://music.163.com/weapi/song/enhance/player/url?csrf_token="
        };

        try {
            for (long quality : qualityLevels) {
                String param = "{\"ids\":[" + id + "],\"br\":" + quality + ",\"csrf_token\":\"\"}";
                String encrypt = com.mengsama.mod.mengsamanetmusic.api.EncryptUtils.encryptedParam(param);
                for (String apiUrl : apiEndpoints) {
                    try {
                        var json = NetWorker.post(apiUrl, encrypt, headers);
                        MengSamaNetMusic.LOGGER.debug("getVIPUrlById: id={} br={} url={} response={}", id, quality, apiUrl, json);
                        var songUrl = getSongUrlFromJson(json);
                        if (songUrl != null && !songUrl.isEmpty()) {
                            MengSamaNetMusic.LOGGER.info("getVIPUrlById: resolved URL for id={} br={}: {}", id, quality, songUrl);
                            return songUrl;
                        }
                    } catch (Exception e) {
                        MengSamaNetMusic.LOGGER.warn("getVIPUrlById: request failed for id={} br={} url={}: {}", id, quality, apiUrl, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.warn("getVIPUrlById failed for id {}: {}", id, e.getMessage());
        }
        MengSamaNetMusic.LOGGER.warn("getVIPUrlById: all quality levels failed for id {}", id);
        return null;
    }

    private static String getSongUrlFromJson(String json) {
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            if (!el.isJsonObject()) return null;
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            var dataEl = obj.get("data");
            if (dataEl == null || !dataEl.isJsonArray() || dataEl.getAsJsonArray().size() == 0) return null;
            com.google.gson.JsonObject first = dataEl.getAsJsonArray().get(0).getAsJsonObject();
            var urlEl = first.get("url");
            return urlEl != null && !urlEl.isJsonNull() ? urlEl.getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
