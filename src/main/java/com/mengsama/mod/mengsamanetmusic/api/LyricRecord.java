package com.mengsama.mod.mengsamanetmusic.api;

import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

import org.jetbrains.annotations.Nullable;

public class LyricRecord {
    private final Int2ObjectSortedMap<String> lyric;
    @Nullable
    private final Int2ObjectSortedMap<String> transformLyric;

    public LyricRecord(Int2ObjectSortedMap<String> lyric, @Nullable Int2ObjectSortedMap<String> transformLyric) {
        this.lyric = lyric;
        this.transformLyric = transformLyric;
    }

    public Int2ObjectSortedMap<String> getLyric() {
        return lyric;
    }

    @Nullable
    public Int2ObjectSortedMap<String> getTransformLyric() {
        return transformLyric;
    }
}
