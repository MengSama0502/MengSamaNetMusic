package com.mengsama.mod.mengsamanetmusic.api;

public class NetEaseSearchResult {
    private final String songId;
    private final String songName;
    private final String artistName;
    private final boolean isVip;

    public NetEaseSearchResult(String songId, String songName, String artistName, boolean isVip) {
        this.songId = songId;
        this.songName = songName;
        this.artistName = artistName;
        this.isVip = isVip;
    }

    public String getSongId() { return songId; }
    public String getSongName() { return songName; }
    public String getArtistName() { return artistName; }
    public boolean isVip() { return isVip; }
    public String getDisplayText() {
        if (artistName != null && !artistName.isEmpty()) {
            return songName + " - " + artistName;
        }
        return songName;
    }
}
