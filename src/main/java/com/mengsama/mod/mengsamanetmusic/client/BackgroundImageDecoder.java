package com.mengsama.mod.mengsamanetmusic.client;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Normalizes ImageIO RGB/BGR/CMYK output and applies JPEG EXIF orientation before texture upload. */
public final class BackgroundImageDecoder {
    private BackgroundImageDecoder() {}

    public static BufferedImage read(Path path) throws IOException {
        BufferedImage source = ImageIO.read(path.toFile());
        if (source == null) throw new IOException("图片数据损坏或没有可用解码器");
        BufferedImage oriented = applyOrientation(source, jpegOrientation(Files.readAllBytes(path)));
        BufferedImage argb = new BufferedImage(oriented.getWidth(), oriented.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argb.createGraphics();
        try { graphics.drawImage(oriented, 0, 0, null); } finally { graphics.dispose(); }
        return argb;
    }

    static int jpegOrientation(byte[] data) {
        try {
            if (data.length < 4 || (data[0] & 255) != 0xFF || (data[1] & 255) != 0xD8) return 1;
            int p = 2;
            while (p + 4 <= data.length) {
                if ((data[p] & 255) != 0xFF) break;
                int marker = data[p + 1] & 255;
                int length = ((data[p + 2] & 255) << 8) | (data[p + 3] & 255);
                if (length < 2 || p + 2 + length > data.length) break;
                if (marker == 0xE1 && length >= 14 && data[p + 4] == 'E' && data[p + 5] == 'x') {
                    int tiff = p + 10;
                    ByteOrder order = data[tiff] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
                    ByteBuffer b = ByteBuffer.wrap(data).order(order);
                    int ifd = tiff + b.getInt(tiff + 4);
                    int count = b.getShort(ifd) & 0xFFFF;
                    for (int i = 0; i < count; i++) {
                        int entry = ifd + 2 + i * 12;
                        if ((b.getShort(entry) & 0xFFFF) == 0x0112) return b.getShort(entry + 8) & 0xFFFF;
                    }
                }
                p += 2 + length;
            }
        } catch (RuntimeException ignored) {}
        return 1;
    }

    static BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) return source;
        boolean swap = orientation >= 5;
        int width = swap ? source.getHeight() : source.getWidth();
        int height = swap ? source.getWidth() : source.getHeight();
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int tx = x, ty = y;
            switch (orientation) {
                case 2 -> tx = source.getWidth() - 1 - x;
                case 3 -> { tx = source.getWidth() - 1 - x; ty = source.getHeight() - 1 - y; }
                case 4 -> ty = source.getHeight() - 1 - y;
                case 5 -> { tx = y; ty = x; }
                case 6 -> { tx = source.getHeight() - 1 - y; ty = x; }
                case 7 -> { tx = source.getHeight() - 1 - y; ty = source.getWidth() - 1 - x; }
                case 8 -> { tx = y; ty = source.getWidth() - 1 - x; }
            }
            target.setRGB(tx, ty, source.getRGB(x, y));
        }
        return target;
    }
}
