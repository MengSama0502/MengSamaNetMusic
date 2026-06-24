package com.mengsama.mod.mengsamanetmusic.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NetEaseMusicList {
    @SerializedName("playlist")
    private PlayList playList;

    public PlayList getPlayList() {
        return playList;
    }

    public static class PlayList {
        @SerializedName("tracks")
        private List<Track> tracks;

        @SerializedName("trackIds")
        private List<TrackId> trackIds;

        public List<Track> getTracks() {
            return tracks;
        }

        public List<TrackId> getTrackIds() {
            return trackIds;
        }
    }

    public static class Track {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        @SerializedName("dt")
        private int dt;

        @SerializedName("ar")
        private List<Artist> ar;

        @SerializedName("al")
        private Album al;

        @SerializedName("fee")
        private int fee;

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getDt() {
            return dt;
        }

        public List<Artist> getAr() {
            return ar;
        }

        public Album getAl() {
            return al;
        }

        public int getFee() {
            return fee;
        }
    }

    public static class TrackId {
        @SerializedName("id")
        private long id;

        public long getId() {
            return id;
        }
    }

    public static class Artist {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Album {
        @SerializedName("id")
        private long id;

        @SerializedName("name")
        private String name;

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
