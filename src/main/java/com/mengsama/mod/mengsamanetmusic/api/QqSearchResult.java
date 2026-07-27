package com.mengsama.mod.mengsamanetmusic.api;

import java.util.Objects;

/** Normalized catalog row returned by the QQ provider adapter. */
public final class QqSearchResult {
    private final String trackKey;
    private final String trackName;
    private final boolean restricted;
    private final String artistLabel;
    private final String albumMid;
    private final String albumPmId;
    private final String coverUrl;
    private final String albumName;
    private final int duration;

    public QqSearchResult(String id, String title, String singer, boolean isVip) {
        this(id, title, singer, isVip, "", "", "", "", 0);
    }

    public QqSearchResult(String id, String title, String singer, boolean isVip,
                          String albumMid, String albumPmId, String coverUrl) {
        this(id, title, singer, isVip, albumMid, albumPmId, coverUrl, "", 0);
    }

    public QqSearchResult(String id, String title, String singer, boolean isVip,
                          String albumMid, String albumPmId, String coverUrl, String albumName, int duration) {
        this.trackKey = normalize(id);
        this.trackName = normalize(title);
        this.restricted = isVip;
        this.artistLabel = normalize(singer);
        this.albumMid = normalize(albumMid);
        this.albumPmId = normalize(albumPmId);
        this.coverUrl = normalize(coverUrl);
        this.albumName = normalize(albumName);
        this.duration = Math.max(0, duration);
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }

    public String getTitle() {
        return trackName;
    }

    public boolean isVip() {
        return restricted;
    }

    public String getId() {
        return trackKey;
    }

    public String getSinger() {
        return artistLabel;
    }

    public String getAlbumMid() { return albumMid; }
    public String getAlbumPmId() { return albumPmId; }
    public String getCoverUrl() { return coverUrl; }
    public String getAlbumName() { return albumName; }
    public int getDuration() { return duration; }

    public String getDisplayText() {
        if (trackName.isEmpty()) return "空";
        return artistLabel.isEmpty() ? trackName : String.join(" - ", trackName, artistLabel);
    }
}
