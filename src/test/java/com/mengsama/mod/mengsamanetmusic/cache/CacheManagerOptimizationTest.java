package com.mengsama.mod.mengsamanetmusic.cache;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerOptimizationTest {
    @Test void cacheValidationReadsAtMost1024Bytes() throws Exception {
        byte[] mp3 = new byte[2 * 1024 * 1024];
        mp3[0] = 'I'; mp3[1] = 'D'; mp3[2] = '3';
        CountingInputStream input = new CountingInputStream(new ByteArrayInputStream(mp3));

        byte[] header = CacheManager.readHeader(input);

        assertEquals(1024, header.length);
        assertEquals(1024, input.bytesRead);
        assertFalse(CacheManager.isHtmlOrErrorResponse(header));
    }

    @Test void htmlErrorIsDetectedFromHeaderWithoutReadingBody() throws Exception {
        byte[] response = new byte[32 * 1024];
        byte[] html = "<!DOCTYPE html><html><body>404</body></html>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(html, 0, response, 0, html.length);
        CountingInputStream input = new CountingInputStream(new ByteArrayInputStream(response));

        assertTrue(CacheManager.isHtmlOrErrorResponse(CacheManager.readHeader(input)));
        assertEquals(1024, input.bytesRead);
    }

    @Test void tickUsesConcurrentIteratorAndSavesCompletedBatchOnce() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/mengsama/mod/mengsamanetmusic/cache/CacheManager.java"));
        int tickStart = source.indexOf("public static void tick()");
        int tickEnd = source.indexOf("public static float getDownloadProgress", tickStart);
        String tickBody = source.substring(tickStart, tickEnd);
        assertTrue(tickBody.contains("for (var iterator = threads.iterator()"));
        assertFalse(tickBody.contains("new ArrayList<>(threads)"));
        assertEquals(1, count(tickBody, "save();"));
    }

    private static int count(String source, String token) {
        int result = 0;
        for (int index = source.indexOf(token); index >= 0; index = source.indexOf(token, index + token.length())) result++;
        return result;
    }

    private static final class CountingInputStream extends FilterInputStream {
        int bytesRead;
        CountingInputStream(InputStream input) { super(input); }
        @Override public int read() throws IOException {
            int value = super.read();
            if (value >= 0) bytesRead++;
            return value;
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = super.read(bytes, offset, length);
            if (count > 0) bytesRead += count;
            return count;
        }
    }
}
