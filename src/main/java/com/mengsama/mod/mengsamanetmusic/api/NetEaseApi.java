package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.util.HashUtil;
import com.mengsama.mod.mengsamanetmusic.util.NetWorker;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NetEaseApi {
    private static final Gson GSON = new Gson();

    private final Map<String, String> requestPropertyData = new HashMap<>() {{
        put("Content-Type", "application/x-www-form-urlencoded");
        put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36");
        put("Origin", "http://music.163.com");
        put("Referer", "http://music.163.com/");
    }};

    public String getPlayInfo(long musicId, NetEaseMusicLevel level) throws Exception {
        String url = String.format("http://music.163.com/api/song/enhance/player/url/v1?encodeType=flac&ids=[%d]&level=%s", musicId, level.toString().toLowerCase());
        return NetWorker.get(url, getRequestPropertyData());
    }

    public Map<String, String> getRequestPropertyData() {
        return requestPropertyData;
    }

    public void setCookie(String cookie) {
        if (cookie != null && !cookie.isBlank()) {
            getRequestPropertyData().put("Cookie", cookie);
            MengSamaNetMusic.LOGGER.info("NetEase Cookie set (length={})", cookie.length());
        } else {
            getRequestPropertyData().remove("Cookie");
            MengSamaNetMusic.LOGGER.warn("NetEase Cookie cleared (VIP songs may not play)");
        }
    }

    public String getQRKey() throws IOException {
        String url = "https://music.163.com/api/login/qrcode/unikey?type=3";
        return BetterNetWorker.get(url, getRequestPropertyData()).body;
    }

    public BetterNetWorker.HttpResponse checkQRLoginStatus(String key) throws IOException {
        String url = String.format("https://music.163.com/api/login/qrcode/client/login?key=%s&type=3", key);
        return BetterNetWorker.get(url, getRequestPropertyData());
    }

    public String sendCaptcha(String phone) throws IOException {
        String url = String.format("https://music.163.com/api/sms/captcha/sent?cellphone=%s&ctcode=86", phone);
        return NetWorker.get(url, getRequestPropertyData());
    }

    public BetterNetWorker.HttpResponse phoneCaptchaLogin(String phone, String captcha) throws IOException {
        String url = String.format("https://music.163.com/api/w/login/cellphone?phone=%s&countrycode=86&rememberLogin=true&captcha=%s&https=true&type=1", phone, captcha);
        return BetterNetWorker.get(url, getRequestPropertyData());
    }

    public BetterNetWorker.HttpResponse emailLogin(String email, String password) throws IOException {
        String url = String.format("https://music.163.com/api/w/login/?username=%s&rememberLogin=true&password=%s&https=true&type=0", email, HashUtil.md5(password));
        return BetterNetWorker.get(url, getRequestPropertyData());
    }

    public String search(String key, int type, int limit) throws IOException {
        String url = String.format("https://music.163.com/api/search/get/web?s=%s&type=%d&limit=%d",
                com.google.common.net.UrlEscapers.urlPathSegmentEscaper().escape(key), type, limit);
        return NetWorker.get(url, getRequestPropertyData());
    }

    public String list(long id) throws IOException {
        String url = String.format("http://music.163.com/api/playlist/detail?id=%d", id);
        return NetWorker.get(url, getRequestPropertyData());
    }

    public String songs(long[] ids) throws IOException {
        String idsStr = StringUtils.deleteWhitespace(Arrays.toString(ids));
        String url = "http://music.163.com/api/song/detail/?ids=" + URLEncoder.encode(idsStr, "UTF-8");
        return NetWorker.get(url, getRequestPropertyData());
    }

    public String song(long songId) throws IOException {
        String url = "http://music.163.com/api/song/detail/?id=" + songId + "&ids=%5B" + songId + "%5D";
        return NetWorker.get(url, getRequestPropertyData());
    }

    public String lyric(long songId) throws IOException {
        String url = "http://music.163.com/api/song/lyric/?id=" + songId + "&lv=-1&kv=-1&tv=-1";
        return NetWorker.get(url, getRequestPropertyData());
    }

    public String dj(long djId) throws IOException {
        String url = String.format("http://music.163.com/api/dj/program/detail?id=%d", djId);
        return NetWorker.get(url, getRequestPropertyData());
    }

    public SongInfo get163Song(long id) throws IOException {

        SongInfo info = null;
        try {
            NetEaseMusicSong pojo = GSON.fromJson(song(id), NetEaseMusicSong.class);
            info = new SongInfo(pojo, id);
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.warn("NetEase song detail API failed for id {}: {}", id, e.getMessage());
        }

        String metingUrl = MetingApi.getSongUrl(id);
        if (metingUrl != null && !metingUrl.isEmpty()) {
            if (info != null) {
                info.songUrl = metingUrl;
            }
            MengSamaNetMusic.LOGGER.info("Using Meting API URL for song id {}: {}", id, metingUrl);
        }

        if (info != null && info.songUrl != null && !info.songUrl.isEmpty()) {
            info.source = SongInfo.detectSource(info.songUrl);
            if ("unknown".equals(info.source)) info.source = "netease";

            if (info.songId == 0) info.songId = id;
            return info;
        }

        SongInfo metingInfo = MetingApi.getSongInfo(id);
        if (metingInfo != null && metingInfo.songUrl != null && !metingInfo.songUrl.isEmpty()) {
            metingInfo.source = "netease";
            metingInfo.songId = id;
            return metingInfo;
        }

        if (info == null) {
            info = new SongInfo();
            info.songName = "NetEase #" + id;
            info.songUrl = MetingApi.getSongUrl(id);
        }
        info.songId = id;
        return info;
    }

    public SongInfo getDjSong(long djId) throws IOException {
        String result = dj(djId);
        JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
        JsonObject program = jsonObject.getAsJsonObject("program");
        if (program == null) {
            MengSamaNetMusic.LOGGER.error("Failed to get DJ song info, program is null for id: {}", djId);
            return new SongInfo();
        }
        JsonObject mainSong = program.getAsJsonObject("mainSong");
        if (mainSong == null) {
            MengSamaNetMusic.LOGGER.error("Failed to get DJ song info, mainSong is null for id: {}", djId);
            return new SongInfo();
        }
        NetEaseMusicSong.Song song = GSON.fromJson(mainSong.toString(), NetEaseMusicSong.Song.class);
        return new SongInfo(song);
    }
}
