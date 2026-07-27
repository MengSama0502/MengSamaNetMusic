package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side binding and idempotency guard for signed playback URL refreshes. */
public final class PlaybackRefreshSessions {
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private record Session(long generation, String identityKey, String ownerKey, long consumedNonce) { }

    private PlaybackRefreshSessions() { }

    public static void publish(String targetId, long generation, SongInfo song, String ownerKey) {
        if (targetId == null || targetId.isBlank() || song == null) return;
        String identity = song.identityKey();
        if (identity.isBlank()) return;
        SESSIONS.compute(targetId, (ignored, previous) -> {
            if (previous != null && generation < previous.generation) return previous;
            long consumed = previous != null && generation == previous.generation ? previous.consumedNonce : 0L;
            return new Session(generation, identity, ownerKey == null ? "" : ownerKey, consumed);
        });
    }

    /** Atomically accepts a nonce once for the exact target, generation, owner and stable song identity. */
    public static boolean consume(String targetId, long generation, long nonce, SongInfo song, String ownerKey) {
        if (targetId == null || targetId.isBlank() || nonce <= 0L || song == null) return false;
        String identity = song.identityKey();
        if (identity.isBlank()) return false;
        final boolean[] accepted = {false};
        SESSIONS.computeIfPresent(targetId, (ignored, session) -> {
            if (session.generation != generation || session.consumedNonce == nonce
                    || !session.identityKey.equals(identity)
                    || !session.ownerKey.equals(ownerKey == null ? "" : ownerKey)) return session;
            accepted[0] = true;
            return new Session(session.generation, session.identityKey, session.ownerKey, nonce);
        });
        return accepted[0];
    }

    static void resetForTest() {
        SESSIONS.clear();
    }
}
