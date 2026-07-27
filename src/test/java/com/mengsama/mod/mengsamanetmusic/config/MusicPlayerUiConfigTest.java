package com.mengsama.mod.mengsamanetmusic.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MusicPlayerUiConfigTest {
    @Test void migratesLegacyAccentAndSerializesBoundedRgbTheme() {
        var json = JsonParser.parseString("{\"accentColor\":\"#FF123456\",\"screenWidth\":9999}").getAsJsonObject();
        MusicPlayerUiConfig.Values values = MusicPlayerUiConfig.Values.from(json);
        assertEquals(0x123456, values.themeRgb());
        assertEquals(640, values.screenWidth);
        values.applyTheme(0x1ABCDEF);
        assertEquals(0xABCDEF, values.themeRgb());
        assertEquals("#ABCDEF", values.themeColor);
        assertEquals(0xFFABCDEF, values.accent());
    }

    @Test void invalidThemeFallsBackAndContrastStaysReadable() {
        var values = MusicPlayerUiConfig.Values.from(JsonParser.parseString("{\"themeColor\":\"#GGGGGG\"}").getAsJsonObject());
        assertEquals(0x7C6FFF, values.themeRgb());
        values.applyTheme(0xFFFFFF);
        assertEquals(0xFF101018, values.primaryText());
        values.applyTheme(0x000000);
        assertEquals(0xFFFFFFFF, values.primaryText());
    }

    @Test void malformedBackgroundSafelyFallsBackAndSurfaceAlphaTracksIt() {
        var values = MusicPlayerUiConfig.Values.from(JsonParser.parseString(
                "{\"backgroundColor\":\"broken\",\"panelColor\":null}").getAsJsonObject());
        assertEquals(MusicPlayerUiConfig.Values.DEFAULT_BACKGROUND_ARGB, values.background());

        values.applyBackground(0x80112233);
        assertEquals(0x69112233, values.panelSurface());
        assertEquals(0x4F112233, values.listSurface());
        assertEquals(0x69112233, values.listHoverSurface());
        assertEquals(0x76112233, values.popupSurface());
    }

    @Test void copiesAndRestoresBothIndependentColors() {
        MusicPlayerUiConfig.Values values = MusicPlayerUiConfig.Values.defaults();
        values.applyTheme(0x123456);
        values.applyBackground(0x78112233);
        MusicPlayerUiConfig.Values snapshot = values.copy();
        values.applyTheme(0xABCDEF);
        values.applyBackground(0xFFFFFFFF);
        values.restoreColors(snapshot);
        assertEquals(0x123456, values.themeRgb());
        assertEquals(0x78112233, values.background());
    }
}
