package com.mengsama.mod.mengsamanetmusic.cache;

import net.fabricmc.fabric.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicCache {
    private static final Set<String> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final HexFormat HEX = HexFormat.of();
    private static final String MUSIC_EXT = ".mp3";
    private static final String METADATA_EXT = ".json";

    private MusicCache() {
    }

    public static String maybeGetCachedFileUrl(String url, String songName) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            Path file = resolveCacheFile(url, songName);
            if (Files.exists(file)) {
                return file.toUri().toURL().toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void warmCacheAsync(String url, String songName) {
        if (!isHttp(url)) {
            return;
        }
        Path file;
        try {
            file = resolveCacheFile(url, songName);
        } catch (Exception e) {
            return;
        }
        String key = file.toString();
        if (Files.exists(file) || !DOWNLOADING.add(key)) {
            return;
        }
        CompletableFuture.runAsync(() -> download(url, file))
                .whenComplete((unused, throwable) -> DOWNLOADING.remove(key));
    }

    public static String getCachedMetadata(String type, long songId) {
        try {
            Path metadataFile = getMetadataFilePath(type, songId);
            if (Files.exists(metadataFile)) {
                return Files.readString(metadataFile, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void cacheMetadata(String type, long songId, String data) {
        if (data == null || data.isBlank()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Path metadataFile = getMetadataFilePath(type, songId);
                Files.createDirectories(metadataFile.getParent());
                Files.writeString(metadataFile, data, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {
            }
        });
    }

    private static Path getMetadataFilePath(String type, long songId) {
        Path metadataDir = FMLPaths.GAMEDIR.get().resolve("netmusic-metadata").resolve(type);
        return metadataDir.resolve(songId + METADATA_EXT);
    }

    public static boolean hasCache(long songId) {
        Path cacheFile = getCacheFilePath(Long.toString(songId));
        return Files.exists(cacheFile);
    }

    public static String getSongCache(long songId) {
        Path cacheFile = getCacheFilePath(Long.toString(songId));
        if (Files.exists(cacheFile)) {
            try {
                return cacheFile.toUri().toURL().toString();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static void download(String url, Path target) {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            URI uri = URI.create(url);
            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {
            }
        }
    }

    public static Path getCacheFilePath(String songId) {
        Path cacheDir = FMLPaths.GAMEDIR.get().resolve("netmusic-cache");
        return cacheDir.resolve(songId + MUSIC_EXT);
    }

    private static Path resolveCacheFile(String url, String songName) throws Exception {
        String baseName = tryExtractId(url);
        if (baseName == null || baseName.isBlank()) {
            baseName = sanitizeName(songName);
            if (baseName == null || baseName.isBlank()) {
                baseName = hexDigest(url);
            }
        }
        Path cacheDir = FMLPaths.GAMEDIR.get().resolve("netmusic-cache");
        return cacheDir.resolve(baseName + MUSIC_EXT);
    }

    private static boolean isHttp(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static String tryExtractId(String url) {
        try {
            String[] parts = url.split("[?&]id=");
            if (parts.length > 1) {
                String idPart = parts[1].split("&")[0];
                return idPart.replace(".mp3", "");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\-_.]+", "_");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    private static String hexDigest(String url) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(url.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(bytes);
    }
}
