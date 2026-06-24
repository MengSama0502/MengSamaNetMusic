package com.mengsama.mod.mengsamanetmusic.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.net.Proxy;

public class ModConfig {

    public static ForgeConfigSpec.BooleanValue ENABLE_STEREO;
    public static ForgeConfigSpec.EnumValue<Proxy.Type> PROXY_TYPE;
    public static ForgeConfigSpec.ConfigValue<String> PROXY_ADDRESS;

    public static ForgeConfigSpec.BooleanValue ENABLE_PLAYER_LYRICS;
    public static ForgeConfigSpec.BooleanValue ENABLE_MAID_LYRICS;
    public static ForgeConfigSpec.ConfigValue<String> ORIGINAL_PLAYER_LYRICS_COLOR;
    public static ForgeConfigSpec.ConfigValue<String> TRANSLATED_PLAYER_LYRICS_COLOR;
    public static ForgeConfigSpec.ConfigValue<String> ORIGINAL_MAID_LYRICS_COLOR;
    public static ForgeConfigSpec.ConfigValue<String> TRANSLATED_MAID_LYRICS_COLOR;

    public static ForgeConfigSpec.BooleanValue ENABLE_NETMUSIC_CD_GENERATION;
    public static ForgeConfigSpec.BooleanValue ENABLE_VIP_NETMUSIC_CD_GENERATION;

    public static ForgeConfigSpec.ConfigValue<String> NET_EASE_COOKIE;
    public static ForgeConfigSpec.ConfigValue<String> NET_EASE_MUSIC_LEVEL;

    public static ForgeConfigSpec.ConfigValue<String> MUSIC_PROVIDER;
    public static ForgeConfigSpec.ConfigValue<String> QQ_VIP_COOKIE;
    public static ForgeConfigSpec.ConfigValue<String> MUSIC_QUALITY;

    public static ForgeConfigSpec.BooleanValue DEBUG_MODE;
    public static ForgeConfigSpec.BooleanValue ENABLE_MUSIC_HUD;
    public static ForgeConfigSpec.IntValue MUSIC_HUD_X;
    public static ForgeConfigSpec.IntValue MUSIC_HUD_Y;

    public static ForgeConfigSpec init() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        builder.comment("Whether stereo playback is enabled");
        ENABLE_STEREO = builder.define("EnableStereo", true);

        builder.comment("Proxy Type, http and socks are supported");
        PROXY_TYPE = builder.defineEnum("ProxyType", Proxy.Type.DIRECT);

        builder.comment("Proxy Address, such as 127.0.0.1:1080, empty is no proxy");
        PROXY_ADDRESS = builder.define("ProxyAddress", "");

        builder.comment("Whether to enable debug mode (verbose logging)");
        DEBUG_MODE = builder.define("DebugMode", false);
        builder.pop();

        builder.push("lyrics");
        builder.comment("Whether to enable lyrics display in the music player");
        ENABLE_PLAYER_LYRICS = builder.define("EnablePlayerLyrics", true);

        builder.comment("Whether to enable lyrics display for the maid");
        ENABLE_MAID_LYRICS = builder.define("EnableMaidLyrics", true);

        builder.comment("The color of the original lyrics in the music player, in #ARGB format");
        ORIGINAL_PLAYER_LYRICS_COLOR = builder.define("OriginalPlayerLyricsColor", "#FFAAAAAA");

        builder.comment("The color of the translated lyrics in the music player, in #ARGB format");
        TRANSLATED_PLAYER_LYRICS_COLOR = builder.define("TranslatedPlayerLyricsColor", "#FFFFFFFF");

        builder.comment("The color of the original lyrics for the maid, in #ARGB format");
        ORIGINAL_MAID_LYRICS_COLOR = builder.define("OriginalMaidLyricsColor", "#FFAAAAAA");

        builder.comment("The color of the translated lyrics for the maid, in #ARGB format");
        TRANSLATED_MAID_LYRICS_COLOR = builder.define("TranslatedMaidLyricsColor", "#FF000000");
        builder.pop();

        builder.push("sophisticated_backpacks");
        builder.comment("Whether NetMusic CDs can generate inside mob backpacks");
        ENABLE_NETMUSIC_CD_GENERATION = builder.define("EnableNetMusicCDGeneration", false);

        builder.comment("Whether VIP NetMusic CDs can generate inside mob backpacks");
        ENABLE_VIP_NETMUSIC_CD_GENERATION = builder.define("EnableVIPNetMusicCDGeneration", false);
        builder.pop();

        builder.push("netease");
        builder.comment("NetEase Cloud Music Cookie (optional, for song info API)",
                "VIP song playback uses Meting API, no cookie needed",
                "Use /mengsamanetmusic reload to hot-reload after changing");
        NET_EASE_COOKIE = builder.define("NetEaseCookie", "");

        builder.comment("NetEase Cloud Music playback quality level: standard, higher, exhigh, lossless, hires");
        NET_EASE_MUSIC_LEVEL = builder.define("NetEaseMusicLevel", "standard");
        builder.pop();

        builder.push("provider");
        builder.comment("Music provider: netease or qq");
        MUSIC_PROVIDER = builder.define("MusicProvider", "netease");

        builder.comment("QQ Music Cookie for VIP song playback",
                "Server: set here, clients don't need to configure",
                "Single-player/Client: set here on the client",
                "Use /mengsamanetmusic reload to hot-reload after changing");
        QQ_VIP_COOKIE = builder.define("QQVipCookie", "");

        builder.comment("Music quality preference: standard, high, lossless");
        MUSIC_QUALITY = builder.define("MusicQuality", "standard");
        builder.pop();

        builder.push("music_hud");
        builder.comment("Whether to enable the music HUD overlay");
        ENABLE_MUSIC_HUD = builder.define("EnableMusicHUD", true);

        builder.comment("The X position of the music HUD on screen");
        MUSIC_HUD_X = builder.defineInRange("MusicHUDX", 5, 0, 1920);

        builder.comment("The Y position of the music HUD on screen");
        MUSIC_HUD_Y = builder.defineInRange("MusicHUDY", 5, 0, 1080);
        builder.pop();

        return builder.build();
    }
}
