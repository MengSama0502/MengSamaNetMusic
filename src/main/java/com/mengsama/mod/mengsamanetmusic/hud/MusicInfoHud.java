package com.mengsama.mod.mengsamanetmusic.hud;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.cache.CacheManager;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class MusicInfoHud {
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(MengSamaNetMusic.MOD_ID,
            "textures/gui/default.png");

    private static SongInfo info;
    private static ResourceLocation icon;
    @Nullable
    private static Long id = null;
    private static NetMusicListUtil.Lyric lyric;
    private static ItemStack stack;
    private static int slot;
    private static int left = 10;
    private static int top = 10;

    private static Thread thread;
    private static int missedFrameCount = 0;

    public static void render(@NotNull GuiGraphics guiGraphics) {
        if (!isMusicHudEnabled()) return;
        if (isGlobalStopMusic()) return;
        if (Minecraft.getInstance().options.hideGui) return;
        SongInfo localInfo = info;
        if (localInfo == null) return;
        if (localInfo.songName == null || localInfo.songName.isEmpty()) return;

        int tick = getPlayingSoundTick();

        if (tick < 0) {
            missedFrameCount++;

            if (missedFrameCount > 200) {
                clearInfo();
                missedFrameCount = 0;
            }

            tick = 0;
        } else {
            missedFrameCount = 0;
        }

        var font = Minecraft.getInstance().font;
        guiGraphics.blit(Objects.requireNonNullElse(icon, DEFAULT_TEXTURE), left, top, 0, 0, 40, 40, 40, 40);

        var text = "";
        if (localInfo.transName == null || localInfo.transName.isEmpty()) {
            text = localInfo.songName;
        } else {
            text = String.format("%s(%s)", localInfo.songName, localInfo.transName);
        }
        guiGraphics.drawString(font, text, left + 50, top, 0xFFFFFFFF);

        String sourceName = SongInfo.getSourceDisplayName(localInfo.source);
        int sourceColor = SongInfo.getSourceColor(localInfo.source);
        int sourceTextWidth = font.width(sourceName);
        guiGraphics.fill(left + 50, top + font.lineHeight + 1, left + 50 + sourceTextWidth + 6, top + font.lineHeight + 11, 0x55000000);
        guiGraphics.drawString(font, sourceName, left + 53, top + font.lineHeight + 2, sourceColor, false);

        var count = localInfo.songTime;

        if (count > 0) {

            float playedSeconds = tick / 20.0f;
            float progress = clamp(playedSeconds / count, 0, 1);
            var tickWidth = 100;
            int barY = top + font.lineHeight + 14;

            guiGraphics.fill(left + 50, barY, left + 50 + tickWidth,
                    barY + 2, 0xFFAAAAAA);

            guiGraphics.fill(left + 50, barY,
                    (int) (left + 50 + tickWidth * progress),
                    barY + 2, 0xFFFFFFFF);

            if (id != null && CacheManager.getDownloadProgress(id) > 0) {
                guiGraphics.fill(left + 50, barY,
                        (int) (left + 50 + tickWidth * clamp(CacheManager.getDownloadProgress(id), 0, 1)),
                        barY + 2, 0xFF00FF00);
            }

            guiGraphics.drawString(font,
                    String.format("%s/%s",
                            NetMusicListUtil.secondsToMinutesSeconds((int) playedSeconds),
                            NetMusicListUtil.secondsToMinutesSeconds(count)),
                    left + 50 + tickWidth + 5, barY - 3, 0xFFFFFFFF);
        }

        if (lyric != null && count > 0) {
            float currentSecond = tick / 20.0f;
            var lyricPart = lyric.getLyric(currentSecond);
            if (lyricPart != null) {
                int lyricY = top + font.lineHeight * 2 + 8;
                guiGraphics.drawString(font, lyricPart.getA(), left + 50,
                        lyricY, 0xFFFFFFFF);
                if (lyricPart.getB() != null && !lyricPart.getB().isEmpty()) {
                    guiGraphics.drawString(font, lyricPart.getB(), left + 50,
                            lyricY + font.lineHeight + 1, 0xFFFFFFFF);
                }
            }
        }
    }

    private static int getPlayingSoundTick() {

        if (com.mengsama.mod.mengsamanetmusic.util.NetMusicSound.currentTick >= 0) {
            return com.mengsama.mod.mengsamanetmusic.util.NetMusicSound.currentTick;
        }
        if (com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound.currentTick >= 0) {
            return com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound.currentTick;
        }

        try {
            var sounds = NetMusicListUtil.getTickableSounds();
            for (var sound : sounds) {
                if (sound instanceof com.mengsama.mod.mengsamanetmusic.util.NetMusicSound netMusicSound) {
                    return netMusicSound.getTick();
                }
                if (sound instanceof com.mengsama.mod.mengsamanetmusic.util.PlayerNetMusicSound playerNetMusicSound) {
                    return playerNetMusicSound.getTick();
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static SongInfo getInfo() {
        return info;
    }

    public static NetMusicListUtil.Lyric getLyric() {
        return lyric;
    }

    public static void setPos(int x, int y) {
        left = x;
        top = y;
    }

    public static void setInfo(SongInfo info, @NotNull ItemStack playerStack, int slot) {
        id = null;
        MusicInfoHud.info = info;
        MusicInfoHud.slot = slot;
        missedFrameCount = 0;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (icon != null) {
            Minecraft.getInstance().getTextureManager().release(icon);
            icon = null;
        }
        lyric = null;
        stack = playerStack;
        getData();
        long cacheId = info.songId;
        if (cacheId == 0) {
            try {
                cacheId = NetMusicListUtil.getIdFromUrl(info.songUrl);
            } catch (Exception ignored) {}
        }
        if (cacheId > 0 && !CacheManager.hasCache(cacheId)) {
            var uuid = UUID.randomUUID().toString();
            CacheManager.startImgDownload(cacheId, uuid);
            CacheManager.startSongDownload(cacheId, uuid);
            CacheManager.startLycDownload(cacheId, uuid);
        }
    }

    public static void clearInfo() {
        info = null;
    }

    public static void getData() {
        if (info == null) return;
        long songId = info.songId;

        if (songId == 0) {
            try {
                songId = NetMusicListUtil.getIdFromUrl(info.songUrl);
                info.songId = songId;
            } catch (Exception e) {

            }
        }

        MusicInfoHud.id = songId > 0 ? songId : null;

        if (songId > 0 && CacheManager.hasCache(songId)) {
            lyric = CacheManager.getLycCache(songId);
            var imagePath = CacheManager.getImageCache(songId);
            if (imagePath != null) {
                var resourceLocation = new ResourceLocation(MengSamaNetMusic.MOD_ID,
                        String.format("icon_%s", songId));
                try {
                    Minecraft.getInstance().getTextureManager().register(resourceLocation,
                            NetMusicListUtil.getTextureFromPath(imagePath));
                    icon = resourceLocation;
                } catch (Exception e) {
                    MengSamaNetMusic.LOGGER.warn("Failed to load cached icon: {}", e.getMessage());
                    icon = DEFAULT_TEXTURE;
                }
            } else {
                icon = DEFAULT_TEXTURE;
            }
        }

        try {
            getTextureFromLocal(info);
        } catch (Exception ignored) {}

        if (songId > 0) {
            final long finalSongId = songId;
            thread = new Thread(() -> getDataByThread(finalSongId));
            thread.start();
        }
    }

    private static void getDataByThread(long id) {

        try {
            var iconUrl = NetMusicListUtil.getIconUrl(NetMusicListUtil.getSongJson(id));
            if (Thread.currentThread().isInterrupted()) return;
            var resourceLocation = new ResourceLocation(MengSamaNetMusic.MOD_ID,
                    String.format("icon_%s", id));
            Minecraft.getInstance().execute(() -> {
                try {
                    Minecraft.getInstance().getTextureManager().register(resourceLocation,
                            NetMusicListUtil.getTextureFromURL(iconUrl));
                    icon = resourceLocation;
                } catch (Exception e) {
                    MengSamaNetMusic.LOGGER.warn("Failed to register icon texture: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.warn("Error fetching song icon: {}", e.getMessage());
        }

        try {
            var l = NetMusicListUtil.getLyric(NetMusicListUtil.getLyricJson(id));
            if (Thread.currentThread().isInterrupted()) return;
            if (l != null) {
                lyric = l;
                MengSamaNetMusic.LOGGER.info("Loaded lyric for song id={}: {} lines", id,
                        l.getLyric() != null ? l.getLyric().size() : 0);
            }
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.warn("Error fetching song lyric: {}", e.getMessage());
        }
    }

    private static void getTextureFromLocal(SongInfo info) {
        var musicPath = convertFileURLToPath(info.songUrl);
        if (musicPath != null) {
            var imagePath = getPicturePath(musicPath);
            if (imagePath == null) return;
            var resourceLocation = new ResourceLocation(MengSamaNetMusic.MOD_ID,
                    String.format("icon_%s", UUID.randomUUID().toString().toLowerCase()));
            try {
                Minecraft.getInstance().getTextureManager().register(resourceLocation,
                        NetMusicListUtil.getTextureFromPath(imagePath));
                icon = resourceLocation;
            } catch (IOException ignored) {
            }
        }
    }

    private static Path getPicturePath(Path musicPath) {
        var p1 = musicPath.getParent().resolve(musicPath.getFileName().toFile().getName() + ".png");
        if (p1.toFile().isFile()) return p1;
        p1 = musicPath.getParent().resolve(musicPath.getFileName().toFile().getName() + ".jpg");
        if (p1.toFile().isFile()) return p1;
        p1 = musicPath.getParent().resolve(musicPath.getFileName().toFile().getName() + ".jpeg");
        if (p1.toFile().isFile()) return p1;
        return null;
    }

    private static Path convertFileURLToPath(String urlString) {
        try {
            URL url = new URL(urlString);
            if (!"file".equals(url.getProtocol())) return null;
            URI uri = url.toURI();
            return Paths.get(uri);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isMusicHudEnabled() {
        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            return (boolean) configClass.getField("musicHUD").get(null);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isGlobalStopMusic() {
        try {
            return NetMusicListUtil.globalStopMusic;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isMusicPlayerItem(ItemStack stack) {
        return stack.getItem() instanceof com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
    }

    private static boolean isContainerEmpty(ItemStack stack) {

        return stack.isEmpty();
    }

    public static void setInfoFromPacket(String songName, int timeSecond, String rawUrl, long songId) {
        SongInfo newInfo = new SongInfo();
        newInfo.songName = songName;
        newInfo.songTime = timeSecond;
        newInfo.songUrl = rawUrl;
        newInfo.songId = songId;
        newInfo.source = SongInfo.detectSource(rawUrl);
        applyInfoAndFetch(newInfo);
    }

    public static void setInfoFromPacket(SongInfo fullInfo) {
        SongInfo newInfo = fullInfo.clone();
        if (newInfo.source == null || newInfo.source.isEmpty() || "unknown".equals(newInfo.source)) {
            newInfo.source = SongInfo.detectSource(newInfo.songUrl);
        }
        applyInfoAndFetch(newInfo);
    }

    private static void applyInfoAndFetch(SongInfo newInfo) {
        info = newInfo;
        stack = null;
        slot = -1;
        missedFrameCount = 0;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (icon != null) {
            Minecraft.getInstance().getTextureManager().release(icon);
            icon = null;
        }
        lyric = null;
        getData();
    }
}
