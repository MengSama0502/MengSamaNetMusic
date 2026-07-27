package com.mengsama.mod.mengsamanetmusic.client.audio;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientMusicPlaybackRefreshTest {
    @BeforeEach
    void reset() {
        ClientMusicPlayback.resetForTest();
    }

    @Test
    void blockAndPlayerTargetsBothIssueExactlyOneRefreshForUnexpectedPayload() {
        SongInfo song = new SongInfo("https://music.163.com/song/media/outer/url?id=123.mp3", "song", 1);
        song.source = "netease";
        song.providerId = "123";
        song.songId = 123L;
        song.rawUrl = song.songUrl;
        var failure = new NetMusicAudioStream.UnexpectedAudioPayloadException(
                NetMusicAudioStream.AudioContainer.HTML, "text/html");

        for (String target : new String[]{"block:0,64,0", "player:device-id"}) {
            assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 7L));
            assertTrue(NetMusicAudioStream.shouldRefreshProvider(song, failure));
            long nonce = ClientMusicPlayback.beginRefresh(target, 7L);
            assertNotEquals(0L, nonce);
            assertEquals(0L, ClientMusicPlayback.beginRefresh(target, 7L));
            assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 8L, nonce));
        }
    }

    @Test
    void matchingSeekResponseIsConsumedButOlderResponseIsRejected() throws Exception {
        String target = "block:seek";
        SongInfo song = new SongInfo("https://example.test/song.mp3", "song", 120);
        ClientMusicPlayback.register(target, new Object());
        java.lang.reflect.Field pending = ClientMusicPlayback.class.getDeclaredField("PENDING_SEEKS");
        pending.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> seeks = (java.util.Map<String, Object>) pending.get(null);
        var constructor = Class.forName(ClientMusicPlayback.class.getName() + "$PendingSeek")
                .getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        seeks.put(target, constructor.newInstance(80, song.identityKey(), System.nanoTime()));

        assertTrue(ClientMusicPlayback.consumeSeekResponse(target, 81, song.identityKey()),
                "an older authoritative response must not replace the newer local seek");
        assertTrue(ClientMusicPlayback.consumeSeekResponse(target, 80, song.identityKey()),
                "the matching response must be consumed without replacing the stream");
        assertFalse(ClientMusicPlayback.consumeSeekResponse(target, 80, song.identityKey()));
    }

    @Test
    void pauseStateIsTargetScopedAndStopClearsIt() throws Exception {
        String target = "item:player:slot:device";
        java.lang.reflect.Field paused = ClientMusicPlayback.class.getDeclaredField("PAUSED");
        paused.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Boolean> states = (java.util.Map<String, Boolean>) paused.get(null);

        states.put(target, true);
        assertTrue(ClientMusicPlayback.isPaused(target));
        assertFalse(ClientMusicPlayback.isPaused("other"));
        ClientMusicPlayback.stop(target);
        assertFalse(ClientMusicPlayback.isPaused(target));
    }

    @Test
    void pauseCommandsAreOrderedWithinOnePlaybackGeneration() {
        String target = "block:test:1";
        assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 12L));
        assertTrue(ClientMusicPlayback.acceptPauseCommand(target, 12L, 100L));
        assertFalse(ClientMusicPlayback.acceptPauseCommand(target, 12L, 99L),
                "delayed pause must not override a newer resume");
        assertTrue(ClientMusicPlayback.acceptPauseCommand(target, 12L, 101L));
        assertFalse(ClientMusicPlayback.acceptPauseCommand(target, 11L, 102L),
                "control for replaced playback must be rejected");
    }

    @Test
    void earlyPauseSurvivesUntilSoundAndChannelRegistration() {
        String target = "item:player:2:device";
        assertTrue(ClientMusicPlayback.setPaused(target, true, 8L, 1L));
        assertTrue(ClientMusicPlayback.isPaused(target));
        assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 8L));
        ClientMusicPlayback.register(target, new Object());
        assertTrue(ClientMusicPlayback.isPaused(target));
    }

    @Test
    void pauseForNextPlaybackGenerationIsAcceptedBeforePlayPacketArrives() {
        String target = "item:player:2:device";
        assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 7L));
        assertTrue(ClientMusicPlayback.setPaused(target, true, 8L, 10L),
                "pause from the server's next playback must survive packet reordering");
        assertTrue(ClientMusicPlayback.isPaused(target));
    }

    @Test
    void localPreviewCannotResumeAnAuthoritativePauseWithOlderSequence() {
        String target = "block:test:paused";
        assertTrue(ClientMusicPlayback.acceptServerGeneration(target, 21L));
        assertTrue(ClientMusicPlayback.setPaused(target, true, 21L, 500L));
        assertTrue(ClientMusicPlayback.isPaused(target));

        assertFalse(ClientMusicPlayback.setPaused(target, false, 21L, 499L));
        assertTrue(ClientMusicPlayback.isPaused(target),
                "screen removal or a delayed local preview must not resume authoritative pause");
        ClientMusicPlayback.setPaused(target, false, 21L, 501L);
        assertFalse(ClientMusicPlayback.isPaused(target));
    }

    @Test
    void handheldTargetFollowsPhysicalInstanceAfterInventorySlotChanges() {
        String instance = java.util.UUID.randomUUID().toString();
        String playingTarget = "item:player:2:" + instance;
        String movedTarget = "item:player:7:" + instance;
        Object sound = new Object();
        ClientMusicPlayback.register(playingTarget, sound);

        assertEquals(playingTarget, ClientMusicPlayback.authoritativeTarget(movedTarget));
        assertEquals("unrelated", ClientMusicPlayback.authoritativeTarget("unrelated"));
    }

    @Test
    void refreshFailureCanRetryOnlyAfterANewExplicitPlaybackRequest() {
        assertTrue(ClientMusicPlayback.acceptServerGeneration("target", 3L));
        long nonce = ClientMusicPlayback.beginRefresh("target", 3L);
        assertNotEquals(0L, nonce);
        assertEquals(0L, ClientMusicPlayback.beginRefresh("target", 3L));
        assertFalse(ClientMusicPlayback.acceptServerGeneration("target", 4L, nonce + 1));
        assertTrue(ClientMusicPlayback.acceptServerGeneration("target", 4L, nonce));

        assertEquals(0L, ClientMusicPlayback.beginRefresh("target", 4L),
                "a failed refreshed stream must not start another refresh/play loop");
        assertTrue(ClientMusicPlayback.acceptServerGeneration("target", 5L));
        assertNotEquals(0L, ClientMusicPlayback.beginRefresh("target", 5L));
    }
}
