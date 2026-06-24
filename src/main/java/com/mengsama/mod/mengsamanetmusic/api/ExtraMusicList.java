package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ExtraMusicList {
    @SerializedName("songs")
    private List<NetEaseMusicList.Track> tracks;

    public List<NetEaseMusicList.Track> getTracks() {
        return tracks;
    }
}
