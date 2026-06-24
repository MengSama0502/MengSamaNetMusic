package com.mengsama.mod.mengsamanetmusic.util;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NetWorker {
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private static final HttpClient NO_REDIRECT_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public static String get(String url, Map<String, String> requestPropertyData) throws IOException {
        HttpRequest request = createRequestBuilder(url, requestPropertyData).GET().build();
        return send(request, HttpResponse.BodyHandlers.ofString(UTF_8)).body();
    }

    public static String post(String url, String param, Map<String, String> requestPropertyData) throws IOException {
        String currentUrl = url;
        String currentParam = param;
        for (int i = 0; i < 5; i++) {
            HttpRequest request = createRequestBuilder(currentUrl, requestPropertyData)
                    .POST(HttpRequest.BodyPublishers.ofString(currentParam, UTF_8)).build();
            try {
                HttpResponse<String> response = NO_REDIRECT_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
                int code = response.statusCode();

                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location != null) {
                        MengSamaNetMusic.LOGGER.debug("POST redirect: {} -> {}", currentUrl, location);
                        currentUrl = resolveRedirectLocation(currentUrl, location);
                        continue;
                    }
                }
                return response.body();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while sending POST request", e);
            }
        }
        throw new IOException("Too many redirects for POST: " + url);
    }

    private static String resolveRedirectLocation(String baseUrl, String location) {
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(location);
            return resolved.toString();
        } catch (URISyntaxException e) {
            return location;
        }
    }

    public static String getRedirectUrl(String url, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            connection.setInstanceFollowRedirects(false);
            if (headers != null) {
                headers.forEach(connection::setRequestProperty);
            }
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                String location = connection.getHeaderField("Location");
                if (location != null) {
                    return new URL(u, location).toString();
                }
            }
            return url;
        } catch (IOException e) {
            return url;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpRequest.Builder createRequestBuilder(String url, Map<String, String> requestPropertyData) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
        requestPropertyData.forEach(builder::header);
        return builder;
    }

    public static java.net.Proxy getProxyFromConfig() {
        return java.net.Proxy.NO_PROXY;
    }

    public static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws IOException {
        try {
            return HTTP_CLIENT.send(request, bodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending HTTP request", e);
        }
    }
}
