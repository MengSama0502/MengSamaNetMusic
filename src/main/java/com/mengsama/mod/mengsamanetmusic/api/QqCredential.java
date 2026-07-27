package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.annotations.SerializedName;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Saved QQ Music session data. Field names are fixed by the provider's JSON protocol. */
public final class QqCredential {
    @SerializedName("musicid")
    private String musicId;
    @SerializedName("musickey")
    private String musicKey;
    @SerializedName("keyExpiresIn")
    private long lifetimeSeconds;
    @SerializedName("musickeyCreateTime")
    private long issuedAtSeconds;
    @SerializedName("refresh_key")
    private String refreshKey;
    @SerializedName("refresh_token")
    private String refreshToken;

    public QqCredential() {
        this("", "", 0, 0, "", "");
    }

    public QqCredential(String musicId, String musicKey, long keyExpiresIn,
                        long musicKeyCreateTime, String refreshKey, String refreshToken) {
        this.musicId = clean(musicId);
        this.musicKey = clean(musicKey);
        this.lifetimeSeconds = Math.max(0, keyExpiresIn);
        this.issuedAtSeconds = Math.max(0, musicKeyCreateTime);
        this.refreshKey = clean(refreshKey);
        this.refreshToken = clean(refreshToken);
    }

    private static String clean(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    boolean isExpiredAt(Instant instant) {
        if (lifetimeSeconds <= 0 || issuedAtSeconds <= 0) return false;
        return instant.getEpochSecond() >= issuedAtSeconds + lifetimeSeconds;
    }

    public boolean isExpired() {
        return isExpiredAt(Clock.systemUTC().instant());
    }

    public boolean isValid() {
        return !clean(musicId).isEmpty() && !clean(musicKey).isEmpty() && !isExpired();
    }

    public String toCookieString() {
        return isValid() ? "uin=" + musicId + "; qm_keyst=" + musicKey : "";
    }

    public String getMusicId() { return clean(musicId); }
    public String getMusicKey() { return clean(musicKey); }
    public long getKeyExpiresIn() { return lifetimeSeconds; }
    public long getMusicKeyCreateTime() { return issuedAtSeconds; }
    public String getRefreshKey() { return clean(refreshKey); }
    public String getRefreshToken() { return clean(refreshToken); }
}
