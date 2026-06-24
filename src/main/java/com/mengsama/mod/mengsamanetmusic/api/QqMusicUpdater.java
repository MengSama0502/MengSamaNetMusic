package com.mengsama.mod.mengsamanetmusic.api;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class QqMusicUpdater {
    private static final long REQUEST_COOLDOWN_MS = 30 * 60 * 1000L;
    private static final java.util.concurrent.ConcurrentHashMap<String, RefreshState> REFRESH_STATES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ExecutorService REFRESH_EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(2, new RefreshThreadFactory());

    private QqMusicUpdater() {
    }

    public static void prefetch(String qqInput, int qualityOffset) {
        if (qqInput == null || qqInput.isBlank()) {
            return;
        }
        RefreshState state = REFRESH_STATES.computeIfAbsent(qqInput, ignored -> new RefreshState());
        state.lastRequestAt = System.currentTimeMillis();
    }

    public static SongInfo refreshIfNeeded(String qqInput, int qualityOffset, String vipCookie) {
        if (qqInput == null || qqInput.isBlank()) {
            return null;
        }
        RefreshState state = REFRESH_STATES.computeIfAbsent(qqInput, ignored -> new RefreshState());
        long now = System.currentTimeMillis();

        if (state.cachedSong != null && hasText(state.cachedSong.songUrl)) {
            if (now - state.lastRequestAt >= REQUEST_COOLDOWN_MS) {
                requestRefreshAsync(qqInput, state, now, qualityOffset, vipCookie);
            }
            return state.cachedSong;
        }

        if (!state.inFlight.compareAndSet(false, true)) {
            waitForInFlight(state);
            return state.cachedSong;
        }
        state.lastRequestAt = now;
        SongInfo updated;
        try {
            updated = QqMusicUtils.resolveSong(qqInput, vipCookie, qualityOffset);
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.error("Failed to refresh QQ music url (sync)", e);
            updated = null;
        } finally {
            state.inFlight.set(false);
        }
        if (updated != null && hasText(updated.songUrl)) {
            state.cachedSong = copySongInfo(updated);
        }
        return state.cachedSong;
    }

    private static void waitForInFlight(RefreshState state) {
        int waited = 0;
        while (state.inFlight.get()) {
            if (waited >= 5000) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            waited += 10;
        }
    }

    private static void requestRefreshAsync(String qqInput, RefreshState state, long now, int qualityOffset, String vipCookie) {
        if (!state.inFlight.compareAndSet(false, true)) {
            return;
        }
        state.lastRequestAt = now;
        REFRESH_EXECUTOR.execute(() -> {
            SongInfo updated;
            try {
                updated = QqMusicUtils.resolveSong(qqInput, vipCookie, qualityOffset);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to refresh QQ music url", e);
                updated = null;
            } finally {
                state.inFlight.set(false);
            }
            if (updated != null && hasText(updated.songUrl)) {
                state.cachedSong = copySongInfo(updated);
            }
        });
    }

    private static SongInfo copySongInfo(SongInfo source) {
        SongInfo copy = new SongInfo();
        copy.songUrl = source.songUrl;
        copy.songName = source.songName;
        copy.songTime = source.songTime;
        copy.transName = source.transName;
        copy.vip = source.vip;
        copy.readOnly = source.readOnly;
        if (source.artists != null) {
            copy.artists.addAll(source.artists);
        }
        return copy;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static final class RefreshState {
        private final java.util.concurrent.atomic.AtomicBoolean inFlight = new java.util.concurrent.atomic.AtomicBoolean(false);
        private volatile long lastRequestAt;
        private volatile SongInfo cachedSong;
    }

    private static final class RefreshThreadFactory implements java.util.concurrent.ThreadFactory {
        private int index = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "mengsamanetmusic-qq-refresh-" + index++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
