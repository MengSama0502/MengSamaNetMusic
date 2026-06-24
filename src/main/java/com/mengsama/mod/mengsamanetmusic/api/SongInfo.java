package com.mengsama.mod.mengsamanetmusic.api;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class SongInfo implements Cloneable {
    public String songUrl;
    public String songName;
    public int songTime;
    public String transName = "";
    public boolean vip;
    public boolean readOnly;
    public List<String> artists = new ArrayList<>();

    public String source = "unknown";

    public long songId = 0;

    public SongInfo() {
    }

    public SongInfo(String songUrl, String songName, int songTime) {
        this.songUrl = songUrl;
        this.songName = songName;
        this.songTime = songTime;
    }

    public SongInfo(String songUrl, String songName, int songTime, boolean vip) {
        this.songUrl = songUrl;
        this.songName = songName;
        this.songTime = songTime;
        this.vip = vip;
    }

    public SongInfo(NetEaseMusicSong pojo, long fallbackId) {
        NetEaseMusicSong.Song song = pojo.getSong();
        if (song != null) {
            this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", song.getId());
            this.songName = song.getName();
            this.songTime = song.getDuration() / 1000;
            this.transName = song.getTransName();
            this.vip = song.needVip();
            this.artists = song.getArtists();
            this.songId = song.getId();
        } else {

            this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", fallbackId);
            this.songName = "NetEase #" + fallbackId;
            this.songTime = 0;
            this.songId = fallbackId;
        }
    }

    public SongInfo(NetEaseMusicSong.Song song) {
        this.songUrl = String.format("https://music.163.com/song/media/outer/url?id=%d.mp3", song.getId());
        this.songName = song.getName();
        this.songTime = song.getDuration() / 1000;
        this.transName = song.getTransName();
        this.vip = song.needVip();
        this.artists = song.getArtists();
        this.songId = song.getId();
    }

    public boolean isValid() {
        return songUrl != null && !songUrl.isBlank()
                && songName != null && !songName.isBlank()
                && songTime > 0;
    }

    public static String detectSource(String url) {
        if (url == null || url.isEmpty()) return "unknown";
        if (url.contains("music.163.com") || url.contains("163.com") || url.contains("netease")) {
            return "netease";
        }
        if (url.contains("qq.com") || url.contains("y.qq.com") || url.contains("dl.stream.qqmusic")) {
            return "qq";
        }
        return "unknown";
    }

    public static String getSourceDisplayName(String source) {
        return switch (source) {
            case "netease" -> "\u7F51\u6613\u4E91";
            case "qq" -> "QQ\u97F3\u4E50";
            default -> "\u672A\u77E5";
        };
    }

    public static int getSourceColor(String source) {
        return switch (source) {
            case "netease" -> 0xFFE60026;
            case "qq" -> 0xFF31C27C;
            default -> 0xFFB8B8CC;
        };
    }

    @Override
    public SongInfo clone() {
        try {
            SongInfo copy = (SongInfo) super.clone();
            copy.songUrl = this.songUrl;
            copy.songName = this.songName;
            copy.songTime = this.songTime;
            copy.transName = this.transName;
            copy.vip = this.vip;
            copy.readOnly = this.readOnly;
            copy.source = this.source;
            copy.songId = this.songId;
            copy.artists = this.artists == null ? new ArrayList<>() : new ArrayList<>(this.artists);
            return copy;
        } catch (CloneNotSupportedException e) {
            return new SongInfo();
        }
    }

    public static void serializeNBT(SongInfo info, CompoundTag tag) {
        if (info == null) {
            return;
        }

        tag.putString("url", info.songUrl != null ? info.songUrl : "");
        tag.putString("name", info.songName != null ? info.songName : "");
        tag.putInt("time", info.songTime);
        tag.putString("trans_name", info.transName != null ? info.transName : "");
        tag.putBoolean("vip", info.vip);
        tag.putBoolean("read_only", info.readOnly);
        tag.putString("source", info.source != null ? info.source : "unknown");
        tag.putLong("songId", info.songId);
        if (info.artists != null && !info.artists.isEmpty()) {
            net.minecraft.nbt.ListTag artistList = new net.minecraft.nbt.ListTag();
            for (String artist : info.artists) {
                artistList.add(net.minecraft.nbt.StringTag.valueOf(artist));
            }
            tag.put("artists", artistList);
        }
    }

    public static SongInfo deserializeNBT(CompoundTag tag) {
        if (tag == null) {
            return new SongInfo("", "", 0);
        }
        SongInfo info = new SongInfo();

        info.songUrl = tag.contains("url") ? tag.getString("url") : tag.getString("songUrl");
        info.songName = tag.contains("name") ? tag.getString("name") : tag.getString("songName");
        info.songTime = tag.contains("time") ? tag.getInt("time") : tag.getInt("songTime");
        info.transName = tag.contains("trans_name") ? tag.getString("trans_name") : tag.getString("transName");
        info.vip = tag.getBoolean("vip");
        info.readOnly = tag.contains("read_only") ? tag.getBoolean("read_only") : tag.getBoolean("readOnly");
        info.source = tag.contains("source") ? tag.getString("source") : "unknown";
        info.songId = tag.contains("songId") ? tag.getLong("songId") : 0;
        info.artists = new ArrayList<>();
        if (tag.contains("artists", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag artistList = tag.getList("artists", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < artistList.size(); i++) {
                info.artists.add(artistList.getString(i));
            }
        } else {

            int artistCount = tag.getInt("artistCount");
            for (int i = 0; i < artistCount; i++) {
                String artist = tag.getString("artist_" + i);
                if (!artist.isEmpty()) {
                    info.artists.add(artist);
                }
            }
        }
        return info;
    }
}
