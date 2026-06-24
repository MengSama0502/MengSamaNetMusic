package com.mengsama.mod.mengsamanetmusic.client.audio;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetMusicAudioStream implements AudioStream {
    private static final ExecutorService AUDIO_STREAM_EXECUTOR = Executors.newFixedThreadPool(
            4,
            r -> {
                Thread t = new Thread(r, "MengSamaNetMusic-AudioStream-Downloader");
                t.setDaemon(true);
                return t;
            }
    );

    private final AudioInputStream stream;
    private final int frameSize;
    private final byte[] frame;
    private final int streamingBufferSize;
    private final ConcurrentLinkedQueue<ByteBuffer> audioDataQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private volatile Throwable failed;

    public NetMusicAudioStream(URL url) throws IOException, UnsupportedAudioFileException {
        this(getAudioInputStream(url));
    }

    public NetMusicAudioStream(AudioInputStream audioInputStream) throws UnsupportedAudioFileException {
        AudioFormat originalFormat = audioInputStream.getFormat();

        AudioFormat pcmFormat = getTargetPCMAudioFormat(originalFormat);
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, audioInputStream);

        AudioFormat targetFormat;
        if (originalFormat.getChannels() == 1) {
            targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(), 16, 1, 2, originalFormat.getSampleRate(), false);
        } else {
            targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(), 16, 2, 4, originalFormat.getSampleRate(), false);
        }

        this.stream = AudioSystem.getAudioInputStream(targetFormat, pcmStream);
        this.frameSize = stream.getFormat().getFrameSize();
        this.frame = new byte[frameSize];
        this.streamingBufferSize = calculateBufferSize(stream.getFormat(), 1);

        pumpBuffers(4);
    }

    private static AudioInputStream getAudioInputStream(URL url) throws IOException, UnsupportedAudioFileException {

        String urlStr = url.toString();
        if (com.mengsama.mod.mengsamanetmusic.api.MetingApi.isMetingUrl(urlStr)) {
            return getDirectAudioStream(url);
        }

        URL resolvedUrl = resolveRedirectUrl(url);
        try {
            return AudioSystem.getAudioInputStream(resolvedUrl);
        } catch (UnsupportedAudioFileException e) {

            MengSamaNetMusic.LOGGER.warn("AudioSystem failed to parse, trying Meting fallback for: {}", url);
            AudioInputStream metingStream = tryMetingFallback(url);
            if (metingStream != null) return metingStream;

            HttpURLConnection connection = (HttpURLConnection) resolvedUrl.openConnection();
            Map<String, String> netEaseHeaders = com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.NET_EASE_API.getRequestPropertyData();
            netEaseHeaders.forEach(connection::setRequestProperty);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            try {
                InputStream inputStream = connection.getInputStream();
                return AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(inputStream));
            } catch (IOException ioEx) {
                connection.disconnect();
                throw ioEx;
            }
        } catch (IOException ioEx) {

            MengSamaNetMusic.LOGGER.warn("IOException getting audio stream, trying Meting fallback for: {}", url);
            AudioInputStream metingStream = tryMetingFallback(url);
            if (metingStream != null) return metingStream;
            throw ioEx;
        }
    }

    private static AudioInputStream getDirectAudioStream(URL url) throws IOException, UnsupportedAudioFileException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        try {
            InputStream inputStream = connection.getInputStream();
            return AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(inputStream));
        } catch (Exception e) {
            connection.disconnect();
            throw e;
        }
    }

    private static AudioInputStream tryMetingFallback(URL originalUrl) {
        try {
            String urlStr = originalUrl.toString();
            if (!urlStr.contains("music.163.com")) return null;

            int idIdx = urlStr.indexOf("id=");
            if (idIdx < 0) return null;
            String sub = urlStr.substring(idIdx + 3);
            int dot = sub.indexOf(".mp3");
            if (dot > 0) sub = sub.substring(0, dot);
            long songId = Long.parseLong(sub);

            MengSamaNetMusic.LOGGER.info("Client Meting fallback: trying for id {}", songId);
            String metingUrl = com.mengsama.mod.mengsamanetmusic.api.MetingApi.getSongUrl(songId);
            if (metingUrl == null || metingUrl.isEmpty()) {
                MengSamaNetMusic.LOGGER.warn("Client Meting fallback: getSongUrl returned null for id {}", songId);
                return null;
            }

            MengSamaNetMusic.LOGGER.info("Client Meting fallback: got URL {}", metingUrl);
            URL metingUrlObj = new URI(metingUrl).toURL();
            return getDirectAudioStream(metingUrlObj);
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.warn("Client Meting fallback failed: {}", e.getMessage());
            return null;
        }
    }

    private static URL resolveRedirectUrl(URL url) throws IOException {
        String protocol = url.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            return url;
        }

        Map<String, String> netEaseHeaders = com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic.NET_EASE_API.getRequestPropertyData();
        URL currentUrl = url;
        for (int i = 0; i < 5; i++) {
            try {
                HttpURLConnection connection = (HttpURLConnection) currentUrl.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                netEaseHeaders.forEach(connection::setRequestProperty);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (location != null) {
                        currentUrl = new URI(currentUrl.toString()).resolve(location).toURL();
                        continue;
                    }
                }
                connection.disconnect();
                return currentUrl;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.warn("Failed to resolve redirect for {}: {}", currentUrl, e.getMessage());
                return currentUrl;
            }
        }
        return currentUrl;
    }

    private static int calculateBufferSize(AudioFormat format, int seconds) {
        float bytesPerSample = format.getSampleSizeInBits() / 8f;
        int channels = format.getChannels();
        float sampleRate = format.getSampleRate();
        return (int) (seconds * bytesPerSample * channels * sampleRate);
    }

    private void pumpBuffers(int readCount) {
        try {
            for (int i = 0; i < readCount; i++) {
                ByteBuffer byteBuffer = BufferUtils.createByteBuffer(streamingBufferSize);
                int bytesRead = 0;
                int count;
                do {
                    count = this.stream.read(frame);
                    if (count != -1) {
                        byteBuffer.put(frame, 0, count);
                        bytesRead += count;
                    }
                } while (count != -1 && bytesRead < streamingBufferSize);

                if (byteBuffer.position() > 0) {
                    byteBuffer.flip();
                    audioDataQueue.offer(byteBuffer);
                }
                if (count == -1) {
                    break;
                }
            }
        } catch (Throwable e) {
            MengSamaNetMusic.LOGGER.error("Failed to read audio stream", e);
            this.failed = e;
            try {
                this.stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private AudioFormat getTargetPCMAudioFormat(AudioFormat originalFormat) {
        int sampleSizeInBits = originalFormat.getSampleSizeInBits();
        if (sampleSizeInBits == AudioSystem.NOT_SPECIFIED) {

            sampleSizeInBits = 16;
        }
        int frameSize = (sampleSizeInBits / 8) * originalFormat.getChannels();
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                originalFormat.getSampleRate(), sampleSizeInBits,
                originalFormat.getChannels(), frameSize, originalFormat.getSampleRate(), false);
    }

    @Override
    public AudioFormat getFormat() {
        return stream.getFormat();
    }

    private void loadAudioData() {
        if (failed == null && audioDataQueue.size() < 4 && loading.compareAndSet(false, true)) {
            AUDIO_STREAM_EXECUTOR.submit(() -> {
                try {
                    pumpBuffers(2);
                } finally {
                    loading.set(false);
                }
            });
        }
    }

    @Override
    public ByteBuffer read(int size) {

        loadAudioData();

        if (size <= 0) {
            return null;
        }

        long waitStart = System.currentTimeMillis();
        while (failed == null && audioDataQueue.isEmpty() && System.currentTimeMillis() - waitStart < 5000) {
            loadAudioData();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (audioDataQueue.isEmpty()) {
            return null;
        }

        int bytesToRead = size;
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(size);
        do {
            ByteBuffer buffer = audioDataQueue.peek();
            if (buffer == null) {

                loadAudioData();
                buffer = audioDataQueue.peek();
                if (buffer == null) {
                    break;
                }
            }
            if (buffer.remaining() <= bytesToRead) {
                bytesToRead -= buffer.remaining();
                byteBuffer.put(buffer);
                audioDataQueue.poll();
            } else {
                int oldLimit = buffer.limit();
                buffer.limit(buffer.position() + bytesToRead);
                byteBuffer.put(buffer);
                buffer.limit(oldLimit);
                bytesToRead = 0;
            }
        } while (bytesToRead > 0);
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
