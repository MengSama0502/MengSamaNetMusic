package com.mengsama.mod.mengsamanetmusic.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheManager {
    private static Map<String, String> musicCache = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
    public static final String DIR_NAME = "netMusicListCache";
    public static final String INDEX_FILE_NAME = "index.json";
    public static Path PATH = FMLPaths.CONFIGDIR.get().resolve(DIR_NAME);
    public static final List<FileDownloadThread> threads = new CopyOnWriteArrayList<>();

    public static void firstTimeInit() {
        try {
            if (!Files.isDirectory(PATH)) {
                Files.createDirectory(PATH);
            }
            Files.writeString(PATH.resolve(INDEX_FILE_NAME), "{}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("all")
    public static void load() {
        try {
            musicCache = (Map<String, String>) GSON.fromJson(Files.readString(PATH.resolve(INDEX_FILE_NAME)),
                    TypeToken.get(Object.class));
            checkCache();
        } catch (Exception e) {
            e.printStackTrace();
            firstTimeInit();
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH.resolve(INDEX_FILE_NAME), GSON.toJson(musicCache));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int checkCache(boolean andClear) {
        var keys = new ArrayList<String>();
        musicCache.forEach((k, v) -> {
            var file = PATH.resolve(v + ".mp3");
            try {
                if (!file.toFile().isFile() || isHtmlOrErrorResponse(Files.readAllBytes(file))) keys.add(k);
            } catch (IOException e) {
                keys.add(k);
            }
        });
        keys.forEach(l -> {
            if (andClear) deleteCache(Long.parseLong(l));
            musicCache.remove(l);
        });
        if (!keys.isEmpty()) save();
        return keys.size();
    }

    public static int checkCache() {
        return checkCache(false);
    }

    private static void startDownload(String downloadUrl, long resourceId, String fileType, String uuid) {
        var thread = new FileDownloadThread(downloadUrl, resourceId, fileType, uuid);
        EXECUTOR_SERVICE.submit(thread);
        threads.add(thread);
    }

    public static void startImgDownload(long resourceId, String uuid) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                startDownload(NetMusicListUtil.getIconUrl(NetMusicListUtil.getSongJson(resourceId))
                        .toString(), resourceId, ".png", uuid);
            } catch (Exception ignored) {
            }
        });
    }

    public static void startSongDownload(long resourceId, String uuid) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                startDownload(pasteUrl(resourceId), resourceId, ".mp3", uuid);
            } catch (Exception ignored) {
            }
        });
    }

    public static void startLycDownload(long resourceId, String uuid) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                var lyc = NetMusicListUtil.getLyric(NetMusicListUtil.getLyricJson(resourceId));
                if (lyc == null) return;
                Files.createDirectories(PATH);
                Files.writeString(PATH.resolve(uuid + ".lyc.json"), lyc.toJson(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (Exception ignored) {
            }
        });
    }

    @SuppressWarnings("all")
    public static String pasteUrl(long resourceId) {
        try {
            return NetMusicListUtil.resolveRedirect(new URL(String.format("https://music.163.com/song/media/outer/url?id=%s.mp3", resourceId)), 3, Map.of()).toString();
        } catch (IOException e) {
            return String.format("https://music.163.com/song/media/outer/url?id=%s.mp3", resourceId);
        }
    }

    private static void addCache(long resourceId, String uuid) {
        musicCache.put(String.valueOf(resourceId), uuid);
        save();
    }

    public static boolean hasCache(long resourceId) {
        return musicCache.containsKey(String.valueOf(resourceId));
    }

    public static Path getImageCache(long resourceId) {
        if (!hasCache(resourceId)) return null;
        var path = PATH.resolve(musicCache.get(String.valueOf(resourceId)) + ".png");
        if (path.toFile().isFile()) {
            return path;
        }
        return null;
    }

    public static String getSongCache(long resourceId) {
        if (!hasCache(resourceId)) return null;
        var path = PATH.resolve(musicCache.get(String.valueOf(resourceId)) + ".mp3");
        if (path.toFile().isFile()) {
            return path.toFile().toURI().toString();
        }
        return null;
    }

    public static NetMusicListUtil.Lyric getLycCache(long resourceId) {
        if (!hasCache(resourceId)) return null;
        var path = PATH.resolve(musicCache.get(String.valueOf(resourceId)) + ".lyc.json");
        if (path.toFile().isFile()) {
            try {
                return NetMusicListUtil.Lyric.fromJson(Files.readString(path));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static void tick() {
        List<FileDownloadThread> toRemove = new ArrayList<>();
        for (FileDownloadThread t : threads) {
            if (t.isCompleted()) {
                if (Objects.equals(t.getFileType(), ".mp3")) {
                    addCache(t.getResourceId(), t.getThreadId());
                }
                toRemove.add(t);
            } else if (t.isFailed()) {
                toRemove.add(t);
            }
        }
        threads.removeAll(toRemove);
    }

    public static float getDownloadProgress(long resourceId) {
        for (FileDownloadThread thread : threads) {
            if (thread.getResourceId() == resourceId && Objects.equals(thread.getFileType(), ".mp3")) {
                return thread.getProgress();
            }
        }
        return 0f;
    }

    public static List<FileDownloadThread> getThreads() {
        return new ArrayList<>(threads);
    }

    public static void deleteCache(long resourceId) {
        if (!hasCache(resourceId)) return;
        List<Path> paths = new ArrayList<>();
        paths.add(PATH.resolve(musicCache.get(String.valueOf(resourceId)) + ".lyc.json"));
        paths.add(getImageCache(resourceId));
        paths.add(PATH.resolve(musicCache.get(String.valueOf(resourceId)) + ".mp3"));
        paths.forEach(path -> {
            if (path != null) {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                }
            }
        });
        musicCache.remove(String.valueOf(resourceId));
        save();
    }

    private static final String[] HTML_STARTS = {
            "<!DOCTYPE html",
            "<html",
            "<?xml",
            "{",
            "[",
            "HTTP/",
            "Error",
            "404",
            "500"
    };

    public static boolean isHtmlOrErrorResponse(byte[] data) {
        if (data == null || data.length < 10) {
            return false;
        }
        int checkLength = Math.min(data.length, 1024);
        String fileStart = new String(data, 0, checkLength, StandardCharsets.UTF_8).trim();
        for (String htmlStart : HTML_STARTS) {
            if (fileStart.startsWith(htmlStart)) {
                return true;
            }
        }
        if (fileStart.contains("<head>") ||
                fileStart.contains("<body>") ||
                fileStart.contains("<title>") ||
                fileStart.contains("</html>") ||
                fileStart.contains("DOCTYPE") ||
                fileStart.contains("html>")) {
            return true;
        }
        if ((fileStart.startsWith("{") && fileStart.contains("\"error\"")) ||
                (fileStart.startsWith("[") && fileStart.contains("\"error\""))) {
            return true;
        }
        return false;
    }

    public static class FileDownloadThread extends Thread {
        private final String downloadUrl;
        private final long resourceId;
        private final String threadId;
        private final Path downloadDir;
        private final String fileType;

        private volatile float progress = 0.0f;
        private volatile long totalBytes = 0;
        private volatile long downloadedBytes = 0;
        private volatile boolean completed = false;
        private volatile boolean failed = false;
        private volatile String errorMessage = "";

        public FileDownloadThread(String downloadUrl, long resourceId, String fileType, String id) {
            this.downloadUrl = downloadUrl;
            this.resourceId = resourceId;
            this.threadId = id;
            this.downloadDir = CacheManager.PATH;
            this.fileType = fileType;
            this.setName("DownloadThread-" + threadId);
        }

        @Override
        public void run() {
            try {
                Files.createDirectories(downloadDir);
                String fileName = threadId + fileType + ".tmp";
                Path filePath = downloadDir.resolve(fileName);

                URL url = new URL(downloadUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);

                totalBytes = connection.getContentLengthLong();

                try (java.io.InputStream inputStream = connection.getInputStream();
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(filePath.toFile())) {
                    if (connection.getResponseCode() == 404) {
                        throw new Exception("404 not found");
                    }

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        if (isInterrupted()) {
                            Files.deleteIfExists(filePath);
                            return;
                        }
                        outputStream.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        downloadedBytes = totalRead;
                        if (totalBytes > 0) {
                            progress = (float) totalRead / totalBytes;
                        }
                    }
                }
                Thread.sleep(100);

                String finalFileName = threadId + fileType;
                Path finalPath = downloadDir.resolve(finalFileName);
                Files.move(filePath, finalPath);
                completed = true;
            } catch (Exception e) {
                failed = true;
                errorMessage = e.getMessage();
            }
        }

        public float getProgress() {
            return progress;
        }

        public long getDownloadedBytes() {
            return downloadedBytes;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean isFailed() {
            return failed;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getThreadId() {
            return threadId;
        }

        public long getResourceId() {
            return resourceId;
        }

        public String getFileType() {
            return fileType;
        }
    }
}
