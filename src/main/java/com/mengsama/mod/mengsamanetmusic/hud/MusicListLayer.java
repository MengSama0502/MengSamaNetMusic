package com.mengsama.mod.mengsamanetmusic.hud;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@OnlyIn(Dist.CLIENT)
public class MusicListLayer {
    public static boolean isRender = false;
    public static int index = -1;
    public static int count = 0;

    public static void render(@NotNull GuiGraphics guiGraphics) {
        if (!isRender) {
            index = -1;
            return;
        }
        if (Minecraft.getInstance().options.hideGui) return;

        var p = Minecraft.getInstance().player;
        if (p == null) return;

        var i = p.getMainHandItem();
        if (!isMusicPlayerItem(i)) {
            isRender = false;
            return;
        }

        var disc = MusicPlayerItem.getCurrentCd(i);
        if (disc.isEmpty() || !isMusicListItem(disc)) {
            isRender = false;
            return;
        }

        int m = getSongIndex(disc);
        if (index == -1) {
            index = m;
        }

        var font = Minecraft.getInstance().font;
        var width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        var height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        List<SongInfo> songList = getSongInfoList(disc);
        var pose = guiGraphics.pose();
        int length = getSelectHudCount();
        pose.pushPose();

        float scale = getSelectHudSize();
        count = songList.size();
        if (m == songList.size()) {
            isRender = false;
            return;
        }

        RenderSystem.enableBlend();

        List<Integer> indexList = getIndexList(index, length, count);

        int margin = 2;
        float selectedSize = 1.3f;
        float totalHeight = (indexList.size() - 1) * (font.lineHeight + margin) + font.lineHeight * selectedSize;

        pose.translate(width - 5, height / 2f, 0);
        pose.scale(scale, scale, 1);
        pose.translate(0, -totalHeight / 2, 0);

        float y = 0;
        float b = 0.45f;
        float v = (float) (1 / Math.pow((Math.E * Math.E * b), 1 / (1 - Math.ceil(length / 2f))));

        for (Integer listIndex : indexList) {
            if (listIndex != null) {
                SongInfo songInfo = songList.get(listIndex);
                String name = getMusicText(songInfo);
                MutableComponent text = Component.literal(name);
                if (songInfo.vip) {
                    text.append(Component.literal(" [VIP]").withStyle(ChatFormatting.RED));
                }
                int textWidth = font.width(text);
                pose.pushPose();
                if (listIndex == index) {
                    pose.translate(-(textWidth * selectedSize + 4), y, 0);
                    pose.scale(selectedSize, selectedSize, 1);
                    drawSongText(guiGraphics, font, text, 0, 0, 0xFFFFFFFF);
                    y += font.lineHeight * selectedSize + margin;
                } else {
                    float alpha = (float) (b * Math.pow(v, -Math.abs(listIndex - index) + 1));
                    pose.translate(-(textWidth + 4), y, 0);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                    drawSongText(guiGraphics, font, text, 0, 0, 0xFFFFFF);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    y += font.lineHeight + margin;
                }
                pose.popPose();
            } else {
                y += font.lineHeight + margin;
            }
        }

        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void drawSongText(GuiGraphics guiGraphics, Font font, MutableComponent text, int x, int y, int color) {
        if (isGlowingTextEnabled()) {
            final int glowColor = 0xFF000000;
            Matrix4f matrix = guiGraphics.pose().last().pose();
            MultiBufferSource bufferSource = guiGraphics.bufferSource();
            font.drawInBatch8xOutline(text.getVisualOrderText(), x, y, color, glowColor, matrix, bufferSource, 0xF000F0);
        }
        guiGraphics.drawString(font, text, x, y, color, !isGlowingTextEnabled());
    }

    private static String getMusicText(SongInfo info) {
        if (!isShowArtistEnabled()) {
            boolean transformDown = isToggleMusicTransformDown();
            return transformDown ? getTransName(info) : info.songName;
        }
        var artists = new StringBuilder();
        if (info.artists != null && !info.artists.isEmpty()) {
            artists.append("——");
            artists.append(String.join(", ", info.artists));
        }
        boolean transformDown = isToggleMusicTransformDown();
        return (transformDown ? getTransName(info) : info.songName) + artists;
    }

    private static String getTransName(SongInfo info) {
        if (info.transName.isEmpty()) return info.songName;
        return info.transName;
    }

    private static List<Integer> getIndexList(int current, int length, int total) {
        if (current < 0 || current > total - 1) {
            return IntStream.range(0, length).mapToObj(i -> (Integer) null).toList();
        }
        List<Integer> indexList = new ArrayList<>();
        int half = length / 2;
        for (int i = -half; i <= half; i++) {
            int idx = current + i;
            if (idx < 0 || idx >= total) {
                indexList.add(null);
                continue;
            }
            indexList.add(idx);
        }
        return indexList;
    }

    private static float getSelectHudSize() {
        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            Object val = configClass.getField("selectHudSize").get(null);
            return val instanceof Number ? ((Number) val).floatValue() : 1.0f;
        } catch (Exception e) {
            return 1.0f;
        }
    }

    private static int getSelectHudCount() {
        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            Object val = configClass.getField("selectHudCount").get(null);
            return val instanceof Number ? ((Number) val).intValue() : 5;
        } catch (Exception e) {
            return 5;
        }
    }

    private static boolean isGlowingTextEnabled() {
        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            return (boolean) configClass.getField("glowingText").get(null);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isShowArtistEnabled() {
        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            return (boolean) configClass.getField("selectHudShowArtist").get(null);
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressWarnings("SameReturnValue")
    private static boolean isToggleMusicTransformDown() {
        try {
            Class<?> keyMappingClass = Class.forName("com.mengsama.mod.mengsamanetmusic.util.NetMusicListKeyMapping");
            Object keyMapping = keyMappingClass.getField("TOGGLE_MUSIC_TRANSFORM").get(null);
            if (keyMapping instanceof net.minecraft.client.KeyMapping km) {
                return km.isDown();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isMusicPlayerItem(ItemStack stack) {
        return stack.getItem() instanceof MusicPlayerItem;
    }

    private static boolean isMusicListItem(ItemStack stack) {
        return false;
    }

    private static int getSongIndex(ItemStack disc) {
        return 0;
    }

    private static List<SongInfo> getSongInfoList(ItemStack disc) {
        return java.util.Collections.emptyList();
    }
}
