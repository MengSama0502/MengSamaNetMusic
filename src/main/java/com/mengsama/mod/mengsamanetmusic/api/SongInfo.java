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
    public String providerId = "";
    /** Stable provider/original URL. Never replace this with a signed media URL. */
    public String rawUrl = "";
    /** Persistent artwork URL supplied by the music provider. */
    public String picUrl = "";
    /** QQ Music album MID used to rebuild official cover CDN URLs after reload. */
    public String albumMid = "";
    /** Preferred provider cover URL, kept separately for backwards-compatible picUrl consumers. */
    public String coverUrl = "";
    /** Human-readable album title; albumMid remains the provider identity. */
    public String albumName = "";
    public long songId = 0;
    /** Runtime-only signed/direct media URL for one playback attempt; never serialized to NBT. */
    public transient String resolvedMediaUrl = "";
    /** Runtime-only headers needed by the current playback URL; never serialized to NBT. */
    public transient java.util.Map<String, String> playbackHeaders = new java.util.HashMap<>();

    public SongInfo() {}

    public SongInfo(String songUrl, String songName, int songTime) {
        this.songUrl = songUrl;
        this.songName = songName;
        this.songTime = songTime;
    }

    public SongInfo(String songUrl, String songName, int songTime, boolean vip) {
        this(songUrl, songName, songTime);
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
            this.albumName = song.getAlbumName();
            this.picUrl = song.getAlbumPicUrl();
            this.coverUrl = this.picUrl;
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
        this.albumName = song.getAlbumName();
        this.picUrl = song.getAlbumPicUrl();
        this.coverUrl = this.picUrl;
        this.songId = song.getId();
    }

    public boolean isValid() {
        return songUrl != null && !songUrl.isBlank() && songName != null && !songName.isBlank() && songTime > 0;
    }

    public static String detectSource(String url) {
        if (url == null || url.isEmpty()) return "unknown";
        if (url.contains("music.163.com") || url.contains("163.com") || url.contains("netease")) return "netease";
        if (url.contains("qq.com") || url.contains("y.qq.com") || url.contains("dl.stream.qqmusic")) return "qq";
        if (url.contains("mzstatic.com") || url.contains("itunes.apple.com")) return "apple";
        return "unknown";
    }

    public void normalizeIdentity() {
        if (source == null || source.isBlank()) source = "unknown";
        if (providerId == null) providerId = "";
        if (rawUrl == null) rawUrl = "";
        if (songUrl == null) songUrl = "";
        if ("unknown".equals(source)) source = detectSource(!rawUrl.isBlank() ? rawUrl : songUrl);
        recoverLegacyIdentity();
        if (rawUrl.isBlank()) rawUrl = canonicalUrl(source, providerId, songId, songUrl);
        if ("netease".equals(source)) {
            if (songId <= 0 && providerId.matches("\\d+")) songId = Long.parseLong(providerId);
            if (songId <= 0) songId = extractNeteaseId(!rawUrl.isBlank() ? rawUrl : songUrl);
            if (providerId.isBlank() && songId > 0) providerId = Long.toString(songId);
            if (songId > 0 && isLikelyTemporaryUrl(rawUrl)) rawUrl = neteaseCanonical(songId);
            picUrl = CoverUrlUtil.normalize(picUrl);
            coverUrl = CoverUrlUtil.normalize(coverUrl);
            if (coverUrl.isBlank()) coverUrl = picUrl;
            if (picUrl.isBlank()) picUrl = coverUrl;
        } else if ("qq".equals(source)) {
            if (providerId.isBlank() && !songUrl.startsWith("http")) providerId = songUrl;
            if (!providerId.isBlank() && isLikelyTemporaryUrl(rawUrl)) rawUrl = qqCanonical(providerId);
            if ((coverUrl == null || coverUrl.isBlank()) && albumMid != null && !albumMid.isBlank()) {
                coverUrl = QqMusicUtils.buildAlbumCoverUrl(albumMid, "");
            }
            if ((picUrl == null || picUrl.isBlank()) && coverUrl != null) picUrl = coverUrl;
            if ((coverUrl == null || coverUrl.isBlank()) && picUrl != null) coverUrl = picUrl;
        } else if ("apple".equals(source)) {
            coverUrl = AppleMusicApi.highResolutionArtwork(coverUrl);
            picUrl = AppleMusicApi.highResolutionArtwork(picUrl);
            if (coverUrl == null || coverUrl.isBlank()) coverUrl = picUrl;
            if (picUrl == null || picUrl.isBlank()) picUrl = coverUrl;
        }
    }

    private void recoverLegacyIdentity() {
        String candidate = !rawUrl.isBlank() ? rawUrl : songUrl;
        if ("netease".equals(source)) {
            if (songId <= 0) songId = extractNeteaseId(candidate);
            if (providerId.isBlank() && songId > 0) providerId = Long.toString(songId);
        } else if ("qq".equals(source) && providerId.isBlank()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                    "(?:songmid|song_mid|mid)=([A-Za-z0-9]+)|/songDetail/([A-Za-z0-9]+)",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(candidate);
            if (matcher.find()) providerId = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
    }

    private static String canonicalUrl(String source, String providerId, long songId, String fallback) {
        if ("netease".equals(source) && songId > 0) return neteaseCanonical(songId);
        if ("netease".equals(source) && providerId != null && providerId.matches("\\d+")) {
            return neteaseCanonical(Long.parseLong(providerId));
        }
        if ("qq".equals(source) && providerId != null && !providerId.isBlank()) return qqCanonical(providerId);
        return fallback == null ? "" : fallback;
    }

    private static String neteaseCanonical(long id) {
        return "https://music.163.com/song/media/outer/url?id=" + id + ".mp3";
    }

    private static String qqCanonical(String id) {
        return "https://y.qq.com/n/ryqq/songDetail/" + id;
    }

    public static boolean isLikelyTemporaryUrl(String value) {
        if (value == null || value.isBlank()) return false;
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("vkey=") || lower.contains("sign=") || lower.contains("signature=")
                || lower.contains("token=") || lower.contains("expires=") || lower.contains("expire=")
                || lower.contains("ws.stream.qqmusic.qq.com") || lower.contains("dl.stream.qqmusic.qq.com")
                || lower.contains("m701.music.126.net") || lower.contains("m801.music.126.net");
    }

    public String preferredCoverUrl() {
        normalizeIdentity();
        String preferred = coverUrl != null && !coverUrl.isBlank() ? coverUrl : picUrl;
        if ("apple".equals(source)) return AppleMusicApi.highResolutionArtwork(preferred);
        return "netease".equals(source) ? CoverUrlUtil.forDisplay(preferred) : CoverUrlUtil.normalize(preferred);
    }

    public String identityKey() {
        normalizeIdentity();
        String id = providerId == null ? "" : providerId.trim();
        if (id.isEmpty() && songId > 0) id = Long.toString(songId);
        return id.isEmpty() ? "" : source + ":" + id;
    }

    public boolean sameIdentity(SongInfo other) {
        if (other == null) return false;
        String key = identityKey();
        return !key.isEmpty() && key.equals(other.identityKey());
    }

    public boolean canRefreshProvider() {
        normalizeIdentity();
        String original = rawUrl == null ? "" : rawUrl.toLowerCase(java.util.Locale.ROOT);
        if (original.startsWith("file:") || original.startsWith("jar:") || original.startsWith("cache:")) return false;
        if (original.contains("meting") || original.contains("api.injahow.cn") || original.contains("music.gdstudio.app")) return false;
        return ("qq".equals(source) && providerId != null && !providerId.isBlank())
                || ("netease".equals(source) && (songId > 0 || providerId != null && providerId.matches("\\d+")));
    }

    private static long extractNeteaseId(String url) {
        if (url == null) return 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:[?&]id=)(\\d+)").matcher(url);
        if (!matcher.find()) return 0;
        try { return Long.parseLong(matcher.group(1)); } catch (NumberFormatException ignored) { return 0; }
    }

    public static String getSourceDisplayName(String source) {
        return switch (source) {
            case "netease" -> "\u7F51\u6613\u4E91";
            case "qq" -> "QQ\u97F3\u4E50";
            case "apple" -> "Apple Music";
            default -> "\u672A\u77E5";
        };
    }

    public static int getSourceColor(String source) {
        return switch (source) { case "netease" -> 0xFFE60026; case "qq" -> 0xFF31C27C; case "apple" -> 0xFFFA586A; default -> 0xFFB8B8CC; };
    }

    @Override
    public SongInfo clone() {
        try {
            SongInfo copy = (SongInfo) super.clone();
            copy.songUrl = songUrl;
            copy.songName = songName;
            copy.songTime = songTime;
            copy.transName = transName;
            copy.vip = vip;
            copy.readOnly = readOnly;
            copy.source = source;
            copy.providerId = providerId;
            copy.rawUrl = rawUrl;
            copy.picUrl = picUrl;
            copy.albumMid = albumMid;
            copy.coverUrl = coverUrl;
            copy.albumName = albumName;
            copy.songId = songId;
            copy.resolvedMediaUrl = resolvedMediaUrl;
            copy.playbackHeaders = playbackHeaders == null ? new java.util.HashMap<>() : new java.util.HashMap<>(playbackHeaders);
            copy.artists = artists == null ? new ArrayList<>() : new ArrayList<>(artists);
            return copy;
        } catch (CloneNotSupportedException e) { return new SongInfo(); }
    }

    public static void serializeNBT(SongInfo info, CompoundTag tag) {
        if (info == null) return;
        info.normalizeIdentity();
        tag.putString("url", info.songUrl != null ? info.songUrl : "");
        tag.putString("name", info.songName != null ? info.songName : "");
        tag.putInt("time", info.songTime);
        tag.putString("trans_name", info.transName != null ? info.transName : "");
        tag.putBoolean("vip", info.vip);
        tag.putBoolean("read_only", info.readOnly);
        tag.putString("source", info.source != null ? info.source : "unknown");
        tag.putString("providerId", info.providerId != null ? info.providerId : "");
        tag.putString("rawUrl", info.rawUrl != null ? info.rawUrl : "");
        tag.putString("picUrl", info.picUrl != null ? info.picUrl : "");
        tag.putString("albumMid", info.albumMid != null ? info.albumMid : "");
        tag.putString("coverUrl", info.coverUrl != null ? info.coverUrl : "");
        tag.putString("albumName", info.albumName != null ? info.albumName : "");
        tag.putLong("songId", info.songId);
        if (info.artists != null && !info.artists.isEmpty()) {
            net.minecraft.nbt.ListTag artistList = new net.minecraft.nbt.ListTag();
            for (String artist : info.artists) artistList.add(net.minecraft.nbt.StringTag.valueOf(artist));
            tag.put("artists", artistList);
        }
    }

    public static SongInfo deserializeNBT(CompoundTag tag) {
        if (tag == null) return new SongInfo("", "", 0);
        SongInfo info = new SongInfo();
        info.songUrl = firstString(tag, "url", "songUrl", "Url", "URL", "SongUrl", "SongURL");
        info.songName = firstString(tag, "name", "songName", "Name", "SongName");
        info.songTime = normalizeLegacySongTime(firstNumber(tag, "time", "songTime", "SongTime", "Time"));
        info.transName = firstString(tag, "trans_name", "transName", "TransName");
        info.vip = tag.getBoolean("vip");
        info.readOnly = tag.contains("read_only") ? tag.getBoolean("read_only") : tag.getBoolean("readOnly");
        info.source = firstString(tag, "source", "Source");
        if (info.source.isBlank()) info.source = "unknown";
        info.providerId = firstString(tag, "providerId", "ProviderId", "SongId", "songID");
        info.rawUrl = firstString(tag, "rawUrl", "RawUrl", "providerUrl", "originalUrl");
        if (info.rawUrl.isBlank()) info.rawUrl = info.songUrl;
        info.picUrl = firstString(tag, "picUrl", "PicUrl", "picURL", "PicURL");
        info.albumMid = firstString(tag, "albumMid", "AlbumMid");
        info.coverUrl = firstString(tag, "coverUrl", "CoverUrl", "coverURL", "CoverURL");
        if (info.coverUrl.isBlank()) info.coverUrl = info.picUrl;
        if (info.picUrl.isBlank()) info.picUrl = info.coverUrl;
        info.albumName = firstString(tag, "albumName", "AlbumName", "album", "Album");
        info.songId = firstNumber(tag, "songId", "SongId", "songID");
        info.artists = new ArrayList<>();
        if (tag.contains("artists", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag artistList = tag.getList("artists", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < artistList.size(); i++) info.artists.add(artistList.getString(i));
        } else {
            int artistCount = tag.getInt("artistCount");
            for (int i = 0; i < artistCount; i++) {
                String artist = tag.getString("artist_" + i);
                if (!artist.isEmpty()) info.artists.add(artist);
            }
            if (info.artists.isEmpty()) {
                String legacyArtists = firstString(tag, "Artists", "artists", "Artist", "artist");
                if (!legacyArtists.isBlank()) {
                    for (String artist : legacyArtists.split("\\s*(?:/|、|,|;|&|\\|)\\s*")) {
                        if (!artist.isBlank()) info.artists.add(artist);
                    }
                }
            }
        }
        info.normalizeIdentity();
        return info;
    }

    private static String firstString(CompoundTag tag, String... keys) {
        for (String key : keys) {
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                String value = tag.getString(key);
                if (!value.isBlank()) return value;
            }
        }
        return "";
    }

    private static long firstNumber(CompoundTag tag, String... keys) {
        for (String key : keys) {
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) return tag.getLong(key);
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                try { return Long.parseLong(tag.getString(key).trim()); } catch (NumberFormatException ignored) { }
            }
        }
        return 0L;
    }

    static int normalizeLegacySongTime(long value) {
        if (value <= 0) return 0;
        long seconds = value >= 10_000L ? value / 1000L : value;
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }
}
