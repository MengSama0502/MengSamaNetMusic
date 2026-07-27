package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class QqLoginService {
    private static final String LOGIN_APP_ID = "716027609";
    private static final String MUSIC_APP_ID = "100497308";
    private static final URI QR_IMAGE_ENDPOINT = URI.create("https://xui.ptlogin2.qq.com/ssl/ptqrshow");
    private static final URI QR_STATUS_ENDPOINT = URI.create("https://xui.ptlogin2.qq.com/ssl/ptqrlogin");
    private static final URI AUTH_ENDPOINT = URI.create("https://graph.qq.com/oauth2.0/authorize");
    private static final URI MUSIC_LOGIN_ENDPOINT = URI.create("https://u6.y.qq.com/cgi-bin/musicu.fcg");
    private static final String CALLBACK = "https://y.qq.com/wk_v17/common_login.html?type=QQ&&redirect=";
    private static final String LOGIN_JUMP = "https://graph.qq.com/oauth2.0/login_jump";
    private static final Pattern LOGIN_CALLBACK = Pattern.compile("ptuiCB\\('(\\d+)','[^']*','([^']*)','[^']*','([^']*)'");
    private static final Pattern AUTH_CODE = Pattern.compile("(?:[?&])code=([^&]+)");
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mengsamanetmusic-qq-auth");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .executor(EXECUTOR).connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public enum LoginState {
        IDLE, FETCHING_QR, WAITING_SCAN, AUTHORIZING, LOGGING_IN, SUCCESS, FAILED, QR_EXPIRED
    }

    private QqLoginService() {}

    public static CompletableFuture<byte[]> fetchQrCode() {
        return requestBytes(uriWithQuery(QR_IMAGE_ENDPOINT, Map.of(
                        "appid", LOGIN_APP_ID, "e", "2", "l", "M", "s", "3", "d", "72", "v", "4",
                        "t", Double.toString(Math.random()), "daid", "383", "pt_3rd_aid", MUSIC_APP_ID, "u1", LOGIN_JUMP)), "")
                .thenApply(response -> {
                    String signature = response.headers().allValues("set-cookie").stream()
                            .map(value -> cookie(value, "qrsig")).filter(value -> !value.isBlank()).findFirst().orElse("");
                    if (signature.isBlank()) throw new IllegalStateException("QQ QR response omitted qrsig");
                    QrSession.set(signature);
                    return response.body();
                });
    }

    public static CompletableFuture<LoginState> pollLogin() {
        String signature = QrSession.getQrsig();
        if (signature.isBlank()) return CompletableFuture.completedFuture(LoginState.FAILED);
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("u1", LOGIN_JUMP);
        parameters.put("ptqrtoken", Long.toString(calculatePtqrtoken(signature)));
        parameters.put("ptredirect", "0"); parameters.put("h", "1"); parameters.put("t", "1"); parameters.put("g", "1");
        parameters.put("from_ui", "1"); parameters.put("ptlang", "2052"); parameters.put("js_ver", "25072815");
        parameters.put("js_type", "1"); parameters.put("login_sig", ""); parameters.put("pt_uistyle", "40");
        parameters.put("aid", LOGIN_APP_ID); parameters.put("daid", "383"); parameters.put("pt_3rd_aid", MUSIC_APP_ID);
        return requestText(uriWithQuery(QR_STATUS_ENDPOINT, parameters), "qrsig=" + signature)
                .thenCompose(QqLoginService::interpretStatus)
                .exceptionally(error -> {
                    MengSamaNetMusic.LOGGER.warn("QQ login poll failed", error);
                    return LoginState.FAILED;
                });
    }

    private static CompletableFuture<LoginState> interpretStatus(HttpResponse<String> response) {
        Matcher callback = LOGIN_CALLBACK.matcher(response.body());
        if (!callback.find()) return CompletableFuture.completedFuture(LoginState.WAITING_SCAN);
        if ("65".equals(callback.group(1))) return CompletableFuture.completedFuture(LoginState.QR_EXPIRED);
        if (!"0".equals(callback.group(1))) return CompletableFuture.completedFuture(LoginState.WAITING_SCAN);
        String verificationUrl = callback.group(2).isBlank() ? callback.group(3) : callback.group(2);
        return exchangeBrowserSession(URI.create(verificationUrl));
    }

    private static CompletableFuture<LoginState> exchangeBrowserSession(URI verificationUrl) {
        return requestText(verificationUrl, "").thenCompose(response -> {
            Map<String, String> cookies = response.headers().allValues("set-cookie").stream()
                    .map(QqLoginService::firstCookiePair).filter(pair -> pair.length == 2)
                    .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1], (left, right) -> right));
            String uin = cookies.getOrDefault("pt2gguin", "");
            String oauthToken = cookies.getOrDefault("pt_oauth_token", "");
            String sessionKey = cookies.getOrDefault("p_skey", "");
            if (uin.isBlank() || oauthToken.isBlank() || sessionKey.isBlank()) {
                return CompletableFuture.completedFuture(LoginState.FAILED);
            }
            return requestAuthorizationCode(uin, oauthToken, sessionKey)
                    .thenCompose(code -> code.isBlank() ? CompletableFuture.completedFuture(null) : requestMusicCredential(uin, code))
                    .thenApply(credential -> {
                        if (credential == null || !credential.isValid()) return LoginState.FAILED;
                        QqCredentialManager.save(credential);
                        return LoginState.SUCCESS;
                    });
        });
    }

    private static CompletableFuture<String> requestAuthorizationCode(String uin, String oauthToken, String sessionKey) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("response_type", "code"); form.put("client_id", MUSIC_APP_ID); form.put("redirect_uri", CALLBACK);
        form.put("scope", "get_user_info"); form.put("state", "y_new.top.pop.logout"); form.put("switch", "");
        form.put("from_ptlogin", "1"); form.put("src", "1"); form.put("update_auth", "1"); form.put("openapi", "1010");
        form.put("g_tk", Long.toString(calculateGtk(sessionKey))); form.put("auth_time", Long.toString(System.currentTimeMillis() / 1000));
        String cookieHeader = "p_uin=" + uin + "; pt_oauth_token=" + oauthToken + "; p_skey=" + sessionKey;
        HttpRequest request = HttpRequest.newBuilder(AUTH_ENDPOINT).timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookieHeader).POST(HttpRequest.BodyPublishers.ofString(formEncode(form))).build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenApply(response -> {
            String location = response.headers().firstValue("location").orElse("");
            Matcher matcher = AUTH_CODE.matcher(location);
            return matcher.find() ? matcher.group(1) : "";
        });
    }

    private static CompletableFuture<QqCredential> requestMusicCredential(String uin, String code) {
        JsonObject common = new JsonObject();
        common.addProperty("_channelid", "208"); common.addProperty("_os_version", "6.2.9200-2");
        common.addProperty("authst", ""); common.addProperty("ct", "19"); common.addProperty("cv", "2121");
        common.addProperty("guid", ""); common.addProperty("patch", "118"); common.addProperty("tmeAppID", "qqmusic");
        common.addProperty("tmeLoginType", 2); common.addProperty("uin", uin);
        JsonObject parameters = new JsonObject();
        parameters.addProperty("appid", Integer.parseInt(MUSIC_APP_ID)); parameters.addProperty("code", code);
        parameters.addProperty("deviceName", "minecraft"); parameters.addProperty("forceRefreshToken", 0);
        parameters.addProperty("onlyNeedAccessToken", 0);
        JsonObject operation = new JsonObject();
        operation.addProperty("method", "Login"); operation.addProperty("module", "music.login.LoginServer"); operation.add("param", parameters);
        JsonObject body = new JsonObject(); body.add("comm", common); body.add("music.login.LoginServer.Login", operation);
        HttpRequest request = HttpRequest.newBuilder(MUSIC_LOGIN_ENDPOINT).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> decodeCredential(response.body()));
    }

    static QqCredential decodeCredential(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject operation = root.getAsJsonObject("music.login.LoginServer.Login");
            if (operation == null || operation.get("code").getAsInt() != 0) return null;
            JsonObject data = operation.getAsJsonObject("data");
            return new QqCredential(string(data, "musicid"), string(data, "musickey"), number(data, "keyExpiresIn", 0),
                    number(data, "musickeyCreateTime", System.currentTimeMillis() / 1000),
                    string(data, "refresh_key"), string(data, "refresh_token"));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    static long calculatePtqrtoken(String value) { return djb(value, 0); }
    static long calculateGtk(String value) { return djb(value, 5381); }

    private static long djb(String value, long seed) {
        long hash = seed;
        for (int codePoint : value.codePoints().toArray()) hash = ((hash << 5) + hash + codePoint) & 0x7fffffffL;
        return hash;
    }

    private static CompletableFuture<HttpResponse<byte[]>> requestBytes(URI uri, String cookie) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15)).GET();
        if (!cookie.isBlank()) builder.header("Cookie", cookie);
        return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static CompletableFuture<HttpResponse<String>> requestText(URI uri, String cookie) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET();
        if (!cookie.isBlank()) builder.header("Cookie", cookie);
        return HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static URI uriWithQuery(URI endpoint, Map<String, String> parameters) {
        return URI.create(endpoint + "?" + formEncode(parameters));
    }

    private static String formEncode(Map<String, String> parameters) {
        return parameters.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(Collectors.joining("&"));
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String cookie(String header, String name) { String[] pair = firstCookiePair(header); return pair.length == 2 && pair[0].equals(name) ? pair[1] : ""; }
    private static String[] firstCookiePair(String header) { return header == null ? new String[0] : header.split(";", 2)[0].trim().split("=", 2); }
    private static String string(JsonObject object, String key) { return object != null && object.has(key) ? object.get(key).getAsString() : ""; }
    private static long number(JsonObject object, String key, long fallback) { return object != null && object.has(key) ? object.get(key).getAsLong() : fallback; }

    public static final class QrSession {
        private static volatile String signature = "";
        private static volatile Consumer<LoginState> listener;
        private QrSession() {}
        static void set(String value) { signature = value == null ? "" : value; }
        static String getQrsig() { return signature; }
        public static void setStateListener(Consumer<LoginState> value) { listener = value; }
        public static void notifyState(LoginState state) { Consumer<LoginState> current = listener; if (current != null) current.accept(state); }
        public static void reset() { signature = ""; }
    }
}
