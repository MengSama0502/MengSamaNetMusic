package com.mengsama.mod.mengsamanetmusic.client.audio;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.NetEaseApi;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProviderAudioHeadersTest {
    @Test
    void qqExplicitHeadersAreNotOverwrittenByNetEaseDefaults() throws Exception {
        MengSamaNetMusic.NET_EASE_API = new NetEaseApi();
        MengSamaNetMusic.NET_EASE_API.setCookie("netease-cookie");
        Map<String, String> headers = NetMusicAudioStream.requestHeaders(
                new URL("https://dl.stream.qqmusic.qq.com/test.mp3"),
                Map.of("Referer", "https://y.qq.com/", "Cookie", "qq-cookie"));

        assertEquals("https://y.qq.com/", headers.get("Referer"));
        assertEquals("qq-cookie", headers.get("Cookie"));
        assertFalse(headers.containsValue("netease-cookie"));
    }

    @Test
    void netEaseUsesSharedResolverHeaders() throws Exception {
        MengSamaNetMusic.NET_EASE_API = new NetEaseApi();
        MengSamaNetMusic.NET_EASE_API.setCookie("MUSIC_U=test");
        Map<String, String> headers = NetMusicAudioStream.requestHeaders(
                new URL("https://m801.music.126.net/test.mp3"), Map.of());

        assertTrue(headers.getOrDefault("Referer", "").contains("music.163.com"));
        assertTrue(headers.getOrDefault("Cookie", "").contains("MUSIC_U=test"));
    }
}
