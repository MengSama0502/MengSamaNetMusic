package com.mengsama.mod.mengsamanetmusic.api;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.util.NetWorker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QqMusicUtils {
    private static final String DEFAULT_SIP = "http://ws.stream.qqmusic.qq.com/";
    private static final FileCandidate[] QUALITY_CANDIDATES = new FileCandidate[]{
            new FileCandidate("F000", "flac"),
            new FileCandidate("M800", "mp3"),
            new FileCandidate("M500", "mp3"),
            new FileCandidate("RS02", "mp3"),
            new FileCandidate("C600", "m4a"),
            new FileCandidate("C400", "m4a"),
            new FileCandidate("C200", "m4a"),
            new FileCandidate("C100", "m4a")
    };

    private QqMusicUtils() {
    }

    public static List<QqSearchResult> search(String query) throws Exception {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        var headers = new HashMap<String, String>() {{
            put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
            put("Accept", "application/json, text/plain, */*");
            put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            put("Content-Type", "application/json;charset=utf-8");
            put("Referer", "https://y.qq.com/");
        }};
        String body = "{\"req_1\":{\"method\":\"DoSearchForQQMusicDesktop\",\"module\":\"music.search.SearchCgiService\",\"param\":{\"grp\":1,\"num_per_page\":10,\"page_num\":1,\"query\":\"%s\",\"search_type\":0}}}".formatted(query);
        var response = postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", body, headers);
        var tree = JsonParser.parseString(response).getAsJsonObject();
        var bodyData = tree.getAsJsonObject("req_1").getAsJsonObject("data").getAsJsonObject("body");
        if (bodyData == null) {
            return Collections.emptyList();
        }
        var songList = bodyData.getAsJsonObject("song").getAsJsonArray("list");
        List<QqSearchResult> results = new ArrayList<>();
        for (JsonElement element : songList) {
            var song = element.getAsJsonObject();
            String mid = song.get("mid").getAsString();
            String name = song.get("name").getAsString();
            String singer = "";
            var singerArray = song.getAsJsonArray("singer");
            if (singerArray != null && !singerArray.isEmpty()) {
                singer = singerArray.get(0).getAsJsonObject().get("name").getAsString();
            }
            int interval = song.get("interval").getAsInt();
            boolean vip = false;
            if (song.has("pay")) {
                vip = song.getAsJsonObject("pay").get("pay_play").getAsInt() == 1;
            }
            results.add(new QqSearchResult(mid, name, singer, vip));
        }
        return results;
    }

    public static SongInfo resolveSong(String input, String preferredCookie, int qualityOffset) throws Exception {
        boolean hasCookie = preferredCookie != null && !preferredCookie.isBlank();
        String uin = extractUinFromCookie(preferredCookie);
        TrackInfo trackInfo = getTrackInfoByMid(input, preferredCookie);
        String mediaMid = trackInfo.mediaMid;
        var headers = new HashMap<String, String>() {{
            put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
            put("Accept", "application/json, text/plain, */*");
            put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            put("Content-Type", "application/json;charset=utf-8");
            put("Sec-Fetch-Dest", "empty");
            put("Sec-Fetch-Mode", "cors");
            put("Sec-Fetch-Site", "same-origin");
            put("Referer", "https://y.qq.com/");
        }};
        if (hasCookie) {
            headers.put("Cookie", preferredCookie);
        }

        FileCandidate[] candidates = qualityOffset > 0
                ? java.util.Arrays.copyOfRange(QUALITY_CANDIDATES, qualityOffset, QUALITY_CANDIDATES.length)
                : QUALITY_CANDIDATES;

        for (int i = 0; i < candidates.length; i++) {
            FileCandidate[] singleCandidate = new FileCandidate[]{candidates[i]};
            try {
                JsonObject vkeyData = requestVkeyData(input, mediaMid, headers, singleCandidate, uin);
                String baseUrl = resolveBaseUrl(vkeyData);
                String songPurl = selectBestPurl(vkeyData.getAsJsonArray("midurlinfo"));
                if (songPurl != null && !songPurl.isBlank()) {
                    SongInfo data = new SongInfo();
                    data.songUrl = baseUrl + songPurl;
                    data.songName = trackInfo.songName;
                    data.songTime = trackInfo.interval;
                    data.vip = trackInfo.vip;
                    MengSamaNetMusic.LOGGER.info("QQ Music resolved (candidate {} {}): {} -> {}",
                            i, candidates[i].prefix, data.songName, data.songUrl);
                    return data;
                }
                MengSamaNetMusic.LOGGER.debug("QQ Music candidate {} {} returned empty purl",
                        i, candidates[i].prefix);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.warn("QQ Music candidate {} {} failed: {}",
                        i, candidates[i].prefix, e.getMessage());
            }
        }

        if (trackInfo.vip && !hasCookie) {
            throw new RuntimeException("VIP song requires cookie for playback. Please login via Phone screen or set QQVipCookie in config.");
        }
        throw new RuntimeException("No playable URL found for mid=" + input + " (vip=" + trackInfo.vip + ", hasCookie=" + hasCookie + ")");
    }

    private static String extractUinFromCookie(String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return "0";
        }

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:^|;\\s*)uin=(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(cookie);
        if (matcher.find()) {
            return matcher.group(1);
        }

        pattern = java.util.regex.Pattern.compile("(?:^|;\\s*)(?:wxuin|o2_uin)=(\\d+)");
        matcher = pattern.matcher(cookie);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "0";
    }

    public static SongNameData getSongNameByMid(String mid) {
        TrackInfo info = getTrackInfoByMid(mid, null);
        return new SongNameData(info.songName, info.interval);
    }

    private static TrackInfo getTrackInfoByMid(String mid, String preferredCookie) {
        try {
            var headers = new HashMap<String, String>() {{
                put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
                put("Accept", "application/json, text/plain, */*");
                put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
                put("Content-Type", "application/json;charset=utf-8");
                put("Sec-Fetch-Dest", "empty");
                put("Sec-Fetch-Mode", "cors");
                put("Sec-Fetch-Site", "same-origin");
                put("Referer", "https://y.qq.com/");
            }};
            if (preferredCookie != null && !preferredCookie.isBlank()) {
                headers.put("Cookie", preferredCookie);
            }
            var response = postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", """
                {"req_1":{"module":"music.pf_song_detail_svr","method":"get_song_detail","param":{"song_mid":"%s","song_id":0},"loginUin":"0","comm":{"uin":"0","format":"json","ct":24,"cv":0}}}
                """.formatted(mid), headers);
            var tree = JsonParser.parseString(response).getAsJsonObject();
            var trackInfo = tree.getAsJsonObject("req_1")
                    .getAsJsonObject("data")
                    .getAsJsonObject("track_info");
            var name = trackInfo.get("name").getAsString();
            var interval = trackInfo.get("interval").getAsInt();
            boolean vip = false;
            if (trackInfo.has("pay")) {
                var pay = trackInfo.getAsJsonObject("pay");
                if (pay.has("pay_play")) {
                    vip = pay.get("pay_play").getAsInt() == 1;
                }
            }
            String mediaMid = "";
            if (trackInfo.has("file")) {
                var file = trackInfo.getAsJsonObject("file");
                if (file.has("media_mid")) {
                    mediaMid = file.get("media_mid").getAsString();
                }
            }
            return new TrackInfo(name, interval, mediaMid, vip);
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.error("", e);
        }
        return new TrackInfo("", 0, "", false);
    }

    private static JsonObject requestVkeyData(String songMid, String mediaMid, Map<String, String> requestPropertyData, FileCandidate[] candidates, String uin) throws IOException {
        JsonArray filenameList = new JsonArray();
        JsonArray songMidList = new JsonArray();
        JsonArray songTypeList = new JsonArray();
        for (FileCandidate candidate : candidates) {
            filenameList.add(candidate.buildFilename(mediaMid));
            songMidList.add(songMid);
            songTypeList.add(0);
        }

        JsonObject param = new JsonObject();
        param.add("filename", filenameList);
        param.addProperty("guid", "10000");
        param.add("songmid", songMidList);
        param.add("songtype", songTypeList);
        param.addProperty("uin", uin);
        param.addProperty("loginflag", 1);
        param.addProperty("platform", "20");

        JsonObject req = new JsonObject();
        req.addProperty("module", "vkey.GetVkeyServer");
        req.addProperty("method", "CgiGetVkey");
        req.add("param", param);

        JsonObject comm = new JsonObject();
        comm.addProperty("uin", uin);
        comm.addProperty("format", "json");
        comm.addProperty("ct", 24);
        comm.addProperty("cv", 0);

        JsonObject body = new JsonObject();
        body.add("req_1", req);
        body.addProperty("loginUin", uin);
        body.add("comm", comm);

        var response = postJson("https://u.y.qq.com/cgi-bin/musicu.fcg", body.toString(), requestPropertyData);
        var tree = JsonParser.parseString(response).getAsJsonObject();
        if (getIntOrDefault(tree, "code", -1) != 0) {
            throw new RuntimeException("Vkey request failed");
        }
        return tree.getAsJsonObject("req_1").getAsJsonObject("data");
    }

    private static String resolveBaseUrl(JsonObject data) {
        if (data != null && data.has("sip")) {
            JsonArray sip = data.getAsJsonArray("sip");
            if (sip != null && !sip.isEmpty()) {
                String value = sip.get(0).getAsString();
                if (value != null && !value.isBlank()) {
                    return value.endsWith("/") ? value : value + "/";
                }
            }
        }
        return DEFAULT_SIP;
    }

    private static String selectBestPurl(JsonArray midurlinfo) {
        if (midurlinfo == null) {
            return "";
        }
        for (int i = 0; i < midurlinfo.size(); i++) {
            var info = midurlinfo.get(i).getAsJsonObject();
            if (info != null && info.has("purl")) {
                String purl = info.get("purl").getAsString();
                if (purl != null && !purl.isBlank()) {
                    return purl;
                }
            }
        }
        return "";
    }

    private static String postJson(String url, String body, Map<String, String> requestPropertyData) throws IOException {
        StringBuilder result = new StringBuilder();
        URLConnection connection = new URL(url).openConnection(NetWorker.getProxyFromConfig());

        for (Map.Entry<String, String> entry : requestPropertyData.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (!requestPropertyData.containsKey("Content-Type")) {
            connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        }
        connection.setConnectTimeout(12000);
        connection.setDoOutput(true);
        connection.setDoInput(true);

        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
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

    private static final class FileCandidate {
        private final String prefix;
        private final String extension;

        private FileCandidate(String prefix, String extension) {
            this.prefix = prefix;
            this.extension = extension;
        }

        private String buildFilename(String mediaMid) {
            return prefix + mediaMid + "." + extension;
        }
    }

    private static final class TrackInfo {
        private final String songName;
        private final int interval;
        private final String mediaMid;
        private final boolean vip;

        private TrackInfo(String songName, int interval, String mediaMid, boolean vip) {
            this.songName = songName;
            this.interval = interval;
            this.mediaMid = mediaMid;
            this.vip = vip;
        }
    }
}
