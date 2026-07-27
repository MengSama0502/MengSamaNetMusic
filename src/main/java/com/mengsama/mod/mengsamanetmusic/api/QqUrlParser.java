package com.mengsama.mod.mengsamanetmusic.api;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class QqUrlParser {
    private QqUrlParser() {}

    public static ParsedUrl parse(String input) {
        return parse(input, QqUrlParser::resolveRedirect);
    }

    static ParsedUrl parse(String input, Function<URI, URI> redirectResolver) {
        String candidate = input == null ? "" : input.trim();
        if (candidate.matches("[A-Za-z0-9]{10,16}")) {
            return result(ParsedUrl.ResourceType.SONG, candidate);
        }
        URI uri = toUri(candidate);
        if (!isQqWebUri(uri)) return null;
        String host = lower(uri.getHost());
        if ((host.equals("c.y.qq.com") || host.equals("c6.y.qq.com")) && redirectResolver != null) {
            uri = redirectResolver.apply(uri);
            if (!isQqWebUri(uri)) return null;
        }

        String pathValue = uri.getPath() == null ? "" : uri.getPath();
        String[] segments = Arrays.stream(pathValue.split("/"))
                .filter(segment -> !segment.isBlank()).toArray(String[]::new);
        for (int i = 0; i + 1 < segments.length; i++) {
            ParsedUrl.ResourceType type = switch (lower(segments[i])) {
                case "albumdetail" -> ParsedUrl.ResourceType.ALBUM;
                case "songdetail", "song" -> ParsedUrl.ResourceType.SONG;
                case "playlist" -> ParsedUrl.ResourceType.PLAYLIST;
                default -> null;
            };
            if (type != null && validId(segments[i + 1])) return result(type, segments[i + 1]);
        }

        Map<String, String> query = parseQuery(uri.getRawQuery());
        String id = query.getOrDefault("id", "");
        String path = lower(pathValue);
        if (validId(id) && path.endsWith("taoge.html")) return result(ParsedUrl.ResourceType.PLAYLIST, id);
        if (validId(id) && path.endsWith("playsong.html")) return result(ParsedUrl.ResourceType.SONG, id);
        return null;
    }

    private static ParsedUrl result(ParsedUrl.ResourceType type, String id) {
        return new ParsedUrl(type, id);
    }

    private static URI toUri(String value) {
        if (value.isBlank()) return null;
        try {
            URI uri = URI.create(value);
            return uri.getScheme() == null ? URI.create("https://" + value) : uri;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isQqWebUri(URI uri) {
        if (uri == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            return false;
        }
        String host = lower(uri.getHost());
        return host.equals("y.qq.com") || host.endsWith(".y.qq.com");
    }

    private static URI resolveRedirect(URI source) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) source.toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(8_000);
            connection.getResponseCode();
            return connection.getURL().toURI();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return Map.of();
        return Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(pair -> decode(pair[0]).toLowerCase(Locale.ROOT), pair -> decode(pair[1]), (a, b) -> b));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean validId(String value) {
        return value != null && value.matches("[A-Za-z0-9]+") && value.length() <= 32;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
