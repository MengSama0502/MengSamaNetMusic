package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.util.NetWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class QqMusicUtils {
    private static final String MUSICU = "https://u6.y.qq.com/cgi-bin/musicu.fcg";
    private static final String DETAIL_MUSICU = "https://u.y.qq.com/cgi-bin/musicu.fcg";
    private static final String FALLBACK_STREAM_ROOT = "http://ws.stream.qqmusic.qq.com/";
    private static final List<MediaFormat> FORMATS = List.of(
            new MediaFormat("F000", "flac"), new MediaFormat("M800", "mp3"),
            new MediaFormat("M500", "mp3"), new MediaFormat("RS02", "mp3"),
            new MediaFormat("C600", "m4a"), new MediaFormat("C400", "m4a"),
            new MediaFormat("C200", "m4a"), new MediaFormat("C100", "m4a"));

    private QqMusicUtils() {}

    public static List<QqSearchResult> search(String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        JsonObject parameters = new JsonObject();
        parameters.addProperty("grp", 1);
        parameters.addProperty("num_per_page", 50);
        parameters.addProperty("page_num", 1);
        parameters.addProperty("query", query.trim());
        parameters.addProperty("search_type", 0);
        JsonObject request = serviceCall("music.search.SearchCgiService", "DoSearchForQQMusicDesktop", parameters);
        JsonObject envelope = envelope("1859");
        envelope.add("req", request);
        return parseSearchResponse(sendJson(MUSICU, envelope, browserHeaders(effectiveCookie())), "req");
    }

    static List<QqSearchResult> parseSearchResponse(String payload, String requestKey) {
        try {
            JsonObject root = JsonParser.parseString(Objects.requireNonNullElse(payload, "")).getAsJsonObject();
            JsonObject response = object(root, requestKey);
            JsonObject data = object(response, "data");
            JsonObject body = object(data, "body");
            JsonObject songSection = object(body, "song");
            JsonArray songs = array(songSection, "list");
            if (number(root, "code", -1) != 0 || number(response, "code", 0) != 0 || songs == null) return List.of();
            List<QqSearchResult> decoded = new ArrayList<>();
            for (JsonElement element : songs) {
                if (!element.isJsonObject()) continue;
                SongInfo song = decodeTrack(element.getAsJsonObject());
                if (song == null || song.providerId.isBlank() || song.songName.isBlank()) continue;
                decoded.add(new QqSearchResult(song.providerId, song.songName, String.join("/", song.artists), song.vip,
                        song.albumMid, text(object(element.getAsJsonObject(), "album"), "pmId"), song.coverUrl,
                        song.albumName, song.songTime));
            }
            return List.copyOf(decoded);
        } catch (RuntimeException malformed) {
            MengSamaNetMusic.LOGGER.debug("Malformed QQ search response", malformed);
            return List.of();
        }
    }

    public static SongInfo resolveSong(String songMid, String cookie, int qualityOffset) throws IOException {
        if (songMid == null || songMid.isBlank()) throw new IOException("QQ song MID is missing");
        SongInfo song = fetchTrack(songMid, cookie);
        JsonObject track = requestTrackObject(songMid, cookie);
        String mediaMid = text(object(track, "file"), "media_mid");
        if (mediaMid.isBlank()) mediaMid = songMid;
        String uin = cookieValue(cookie, "uin", "wxuin", "o2_uin");
        if (uin.isBlank()) uin = "0";
        int start = Math.max(0, Math.min(qualityOffset, FORMATS.size() - 1));
        Map<String, String> headers = browserHeaders(cookie);
        for (MediaFormat format : FORMATS.subList(start, FORMATS.size())) {
            JsonObject data = requestPlayback(songMid, mediaMid, uin, format, headers);
            String purl = firstPlayablePath(array(data, "midurlinfo"));
            if (!purl.isBlank()) {
                String streamRoot = firstString(array(data, "sip"), FALLBACK_STREAM_ROOT);
                song.songUrl = joinUrl(streamRoot, purl);
                song.resolvedMediaUrl = song.songUrl;
                song.playbackHeaders.putAll(playbackHeaders(cookie));
                return song;
            }
        }
        if (song.vip && (cookie == null || cookie.isBlank())) throw new IOException("QQ VIP song requires a login cookie");
        throw new IOException("QQ Music returned no playable URL for " + songMid);
    }

    public static SongNameData getSongNameByMid(String mid) {
        try {
            SongInfo song = fetchTrack(mid);
            return new SongNameData(song.songName, song.songTime);
        } catch (IOException error) {
            return new SongNameData("", 0);
        }
    }

    public static SongInfo fetchTrack(String songMid) throws IOException {
        return fetchTrack(songMid, effectiveCookie());
    }

    private static SongInfo fetchTrack(String songMid, String cookie) throws IOException {
        SongInfo song = decodeTrack(requestTrackObject(songMid, cookie));
        if (song == null) throw new IOException("QQ track metadata is missing");
        return song;
    }

    private static JsonObject requestTrackObject(String songMid, String cookie) throws IOException {
        JsonObject parameters = new JsonObject();
        parameters.addProperty("song_mid", songMid);
        parameters.addProperty("song_id", 0);
        JsonObject envelope = envelope("0");
        envelope.add("req_1", serviceCall("music.pf_song_detail_svr", "get_song_detail", parameters));
        JsonObject root = JsonParser.parseString(sendJson(DETAIL_MUSICU, envelope, browserHeaders(cookie))).getAsJsonObject();
        JsonObject response = object(root, "req_1");
        JsonObject data = object(response, "data");
        JsonObject track = object(data, "track_info");
        if (track == null) throw new IOException("QQ track detail response has no track_info");
        return track;
    }

    static SongInfo decodeTrack(JsonObject track) {
        if (track == null) return null;
        String mid = text(track, "mid");
        String title = text(track, "name");
        SongInfo song = new SongInfo(mid, title, number(track, "interval", 0));
        song.providerId = mid;
        song.source = "qq";
        JsonObject album = object(track, "album");
        song.albumMid = text(album, "mid");
        song.albumName = text(album, "name");
        song.coverUrl = buildAlbumCoverUrl(song.albumMid, text(album, "pmId"));
        song.picUrl = song.coverUrl;
        JsonObject pay = object(track, "pay");
        song.vip = number(pay, "pay_play", 0) == 1;
        JsonArray singers = array(track, "singer");
        if (singers != null) {
            for (JsonElement singer : singers) {
                if (singer.isJsonObject()) {
                    String name = text(singer.getAsJsonObject(), "name");
                    if (!name.isBlank()) song.artists.add(name);
                }
            }
        }
        return song;
    }

    static String catalogRequest(String module, String method, JsonObject parameters) throws IOException {
        JsonObject body = envelope("2121");
        body.add("music", serviceCall(module, method, parameters));
        return sendJson(MUSICU, body, browserHeaders(effectiveCookie()));
    }

    public static String getLyric(String songMid) throws IOException {
        if (songMid == null || songMid.isBlank()) return "";
        Map<String, String> headers = browserHeaders(effectiveCookie());
        String endpoint = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid="
                + URLEncoder.encode(songMid, StandardCharsets.UTF_8) + "&format=json&nobase64=1&g_tk=5381";
        return text(JsonParser.parseString(NetWorker.get(endpoint, headers)).getAsJsonObject(), "lyric");
    }

    public static List<String> buildAlbumCoverUrls(String albumMid) {
        String mid = albumMid == null ? "" : albumMid.trim();
        if (mid.isEmpty()) return List.of();
        String prefix = "https://y.gtimg.cn/music/photo_new/T002R";
        return List.of(prefix + "300x300M000" + mid + ".jpg", prefix + "500x500M000" + mid + ".jpg",
                prefix + "150x150M000" + mid + ".jpg", "https://y.qq.com/music/photo_new/T002R300x300M000" + mid + ".jpg");
    }

    public static String buildAlbumCoverUrl(String albumMid, String ignoredPmId) {
        List<String> urls = buildAlbumCoverUrls(albumMid);
        return urls.isEmpty() ? "" : urls.get(0);
    }

    public static Map<String, String> playbackHeaders(String cookie) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        headers.put("Referer", "https://y.qq.com/");
        if (cookie != null && !cookie.isBlank()) headers.put("Cookie", cookie);
        return headers;
    }

    private static JsonObject requestPlayback(String songMid, String mediaMid, String uin, MediaFormat format,
                                               Map<String, String> headers) throws IOException {
        JsonObject parameters = new JsonObject();
        parameters.add("filename", strings(format.fileName(mediaMid)));
        parameters.addProperty("guid", "10000");
        parameters.add("songmid", strings(songMid));
        parameters.add("songtype", integers(0));
        parameters.addProperty("uin", uin);
        parameters.addProperty("loginflag", 1);
        parameters.addProperty("platform", "20");
        JsonObject body = envelope("0");
        body.addProperty("loginUin", uin);
        body.add("req_1", serviceCall("vkey.GetVkeyServer", "CgiGetVkey", parameters));
        JsonObject root = JsonParser.parseString(sendJson(DETAIL_MUSICU, body, headers)).getAsJsonObject();
        JsonObject response = object(root, "req_1");
        JsonObject data = object(response, "data");
        if (number(root, "code", -1) != 0 || data == null) throw new IOException("QQ vkey request failed");
        return data;
    }

    private static JsonObject serviceCall(String module, String method, JsonObject parameters) {
        JsonObject call = new JsonObject();
        call.addProperty("module", module);
        call.addProperty("method", method);
        call.add("param", parameters);
        return call;
    }

    private static JsonObject envelope(String clientVersion) {
        JsonObject common = new JsonObject();
        common.addProperty("uin", "0");
        common.addProperty("format", "json");
        common.addProperty("ct", 19);
        common.addProperty("cv", clientVersion);
        JsonObject body = new JsonObject();
        body.add("comm", common);
        return body;
    }

    private static String sendJson(String endpoint, JsonObject body, Map<String, String> headers) throws IOException {
        URLConnection connection = new URL(endpoint).openConnection(NetWorker.getProxyFromConfig());
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(15_000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        headers.forEach(connection::setRequestProperty);
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream raw = connection.getInputStream(); InputStream decoded = decodeStream(raw, connection.getContentEncoding());
             BufferedReader reader = new BufferedReader(new InputStreamReader(decoded, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            for (String line; (line = reader.readLine()) != null;) response.append(line);
            return response.toString();
        }
    }

    private static InputStream decodeStream(InputStream input, String encoding) throws IOException {
        String value = encoding == null ? "" : encoding.toLowerCase(Locale.ROOT);
        if (value.contains("gzip")) return new GZIPInputStream(input);
        if (value.contains("deflate")) return new InflaterInputStream(input);
        return input;
    }

    private static Map<String, String> browserHeaders(String cookie) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Referer", "https://y.qq.com/");
        headers.put("Origin", "https://y.qq.com");
        if (cookie != null && !cookie.isBlank()) headers.put("Cookie", cookie);
        return headers;
    }

    private static String effectiveCookie() { return VipCookieState.getEffectiveVipCookie(); }

    private static String cookieValue(String cookie, String... keys) {
        if (cookie == null) return "";
        for (String pair : cookie.split(";")) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length != 2) continue;
            for (String key : keys) if (parts[0].equalsIgnoreCase(key)) return parts[1].replaceFirst("^o0*", "");
        }
        return "";
    }

    private static String firstPlayablePath(JsonArray candidates) {
        if (candidates == null) return "";
        for (JsonElement candidate : candidates) {
            if (!candidate.isJsonObject()) continue;
            String path = text(candidate.getAsJsonObject(), "purl");
            if (!path.isBlank()) return path;
        }
        return "";
    }

    private static String firstString(JsonArray values, String fallback) {
        return values != null && !values.isEmpty() && values.get(0).isJsonPrimitive() ? values.get(0).getAsString() : fallback;
    }

    private static String joinUrl(String base, String path) {
        return (base.endsWith("/") ? base : base + "/") + (path.startsWith("/") ? path.substring(1) : path);
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static JsonArray array(JsonObject parent, String key) {
        return parent != null && parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : null;
    }

    private static String text(JsonObject parent, String key) {
        try { return parent != null && parent.has(key) && !parent.get(key).isJsonNull() ? parent.get(key).getAsString() : ""; }
        catch (RuntimeException ignored) { return ""; }
    }

    private static int number(JsonObject parent, String key, int fallback) {
        try { return parent != null && parent.has(key) ? parent.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static JsonArray strings(String value) { JsonArray array = new JsonArray(); array.add(value); return array; }
    private static JsonArray integers(int value) { JsonArray array = new JsonArray(); array.add(value); return array; }

    private record MediaFormat(String prefix, String extension) {
        String fileName(String mediaMid) { return prefix + mediaMid + "." + extension; }
    }
}
