package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackRefreshSessionsTest {
    @BeforeEach
    void reset() {
        PlaybackRefreshSessions.resetForTest();
    }

    @Test
    void acceptsNonceOnceForExactTargetGenerationIdentityAndOwner() {
        SongInfo song = qqSong("mid-a");
        PlaybackRefreshSessions.publish("target", 7L, song, "owner");

        assertTrue(PlaybackRefreshSessions.consume("target", 7L, 11L, song, "owner"));
        assertFalse(PlaybackRefreshSessions.consume("target", 7L, 11L, song, "owner"));
        assertFalse(PlaybackRefreshSessions.consume("target", 6L, 12L, song, "owner"));
        assertFalse(PlaybackRefreshSessions.consume("target", 7L, 12L, song, "other"));
        assertFalse(PlaybackRefreshSessions.consume("target", 7L, 12L, qqSong("mid-b"), "owner"));
    }

    @Test
    void newerGenerationReplacesOldBinding() {
        SongInfo song = qqSong("mid-a");
        PlaybackRefreshSessions.publish("target", 7L, song, "owner");
        PlaybackRefreshSessions.publish("target", 8L, song, "owner");

        assertFalse(PlaybackRefreshSessions.consume("target", 7L, 21L, song, "owner"));
        assertTrue(PlaybackRefreshSessions.consume("target", 8L, 22L, song, "owner"));
    }

    private static SongInfo qqSong(String mid) {
        SongInfo song = new SongInfo("https://y.qq.com/n/ryqq/songDetail/" + mid, "test", 1);
        song.source = "qq";
        song.providerId = mid;
        song.rawUrl = song.songUrl;
        song.normalizeIdentity();
        return song;
    }
}
