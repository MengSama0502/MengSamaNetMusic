package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.network.DeleteMusicDataPacket;
import com.mengsama.mod.mengsamanetmusic.network.ModNetwork;
import com.mengsama.mod.mengsamanetmusic.network.MoveMusicDataPacket;
import com.mengsama.mod.mengsamanetmusic.network.MusicListDataPacket;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MusicSelectionScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/bg.png");
    private static final ResourceLocation CD_TEXTURE = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/cd.png");
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/button.png");

    private final List<String> musicList;
    private final int backgroundWidth = 321;
    private final int backgroundHeight = 161;
    private int left, top;
    private PlayModeButton playModeButton;
    private MusicListWidget listWidget;
    private Integer index;
    private final PlayMode mode;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;

    private float lastScroll = 0;
    private float CDRotation = 0;
    private float nowSpeed = 0;
    private float pointerRotation = 0;

    public MusicSelectionScreen(List<SongInfo> songs, PlayMode mode, int index) {
        super(Component.translatable("gui.mengsamanetmusic.play_list.title"));
        this.musicList = formatSongList(songs);
        this.mode = mode;

        if (songs.isEmpty()) {
            this.index = 0;
        } else if (index < 0 || index > songs.size()) {
            this.index = 0;
        } else {
            this.index = index;
        }
    }

    private static List<String> formatSongList(List<SongInfo> songs) {
        List<String> result = new ArrayList<>();
        for (SongInfo info : songs) {
            if (info.artists.isEmpty()) {
                result.add(info.songName);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String artist : info.artists) {
                    sb.append(artist).append("、");
                }
                String tag = "";
                if (info.readOnly) {
                    tag = Component.translatable("gui.mengsamanetmusic.play_list.read_only").getString();
                    } else if (info.vip) {
                        tag = Component.translatable("gui.mengsamanetmusic.play_list.vip").getString();
                }
                String artists = sb.toString();
                result.add(info.songName + tag + " —— " + artists.substring(0, artists.length() - 1));
            }
        }
        return result;
    }

    @Override
    protected void init() {
        super.init();

        this.left = (this.width - this.backgroundWidth) / 2;
        this.top = (this.height - this.backgroundHeight) / 2;

        listWidget = new MusicListWidget();
        for (String music : musicList) {
            listWidget.addMusicEntry(music);
        }
        listWidget.addMusicEntry(Component.translatable("gui.mengsamanetmusic.play_list.add").getString());

        int safeIndex = Math.min(index, listWidget.children().size() - 1);
        if (safeIndex < 0) safeIndex = 0;
        listWidget.setSelected(listWidget.children().get(safeIndex));
        this.addRenderableWidget(listWidget);

        playModeButton = new PlayModeButton(left + 4 + 3, top + 133, button -> {
            playModeButton.playMode = playModeButton.playMode.getNext();
            playModeButton.setTooltip(Tooltip.create(playModeButton.playMode.getName()));
            sendPackage();
        }, mode);
        this.addRenderableWidget(playModeButton);

        deleteButton = new DeleteButton(left + 4 + 66 + 3, top + 133, button -> deleteMusic());

        upButton = new MoveButton(left + 4 + 22 + 3, top + 133, button -> moveMusic(true), true);
        downButton = new MoveButton(left + 4 + 44 + 3, top + 133, button -> moveMusic(false), false);

        deleteButton.active = canDelete();
        upButton.active = canMove(true);
        downButton.active = canMove(false);

        this.addRenderableWidget(deleteButton);
        this.addRenderableWidget(upButton);
        this.addRenderableWidget(downButton);

        lastScroll = (float) listWidget.getScrollAmount();
        nowSpeed = 0;

        if (musicList.isEmpty() || listWidget.getSelectedIndex() == musicList.size()) {
            pointerRotation = 45;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
        guiGraphics.blit(BACKGROUND_TEXTURE, left, top, 0, 0, backgroundWidth, backgroundHeight, 512, 256);
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        renderCD(context, delta);
        renderPointer(context, delta);
    }

    private void renderCD(@NotNull GuiGraphics guiGraphics, float delta) {
        guiGraphics.pose().pushPose();
        float scrollSpeed = (float) Math.abs(listWidget.getScrollAmount() - lastScroll);
        if (scrollSpeed <= 0.3) scrollSpeed = 0;
        if (Math.abs(scrollSpeed - nowSpeed) > 0.01) {
            nowSpeed += scrollSpeed > nowSpeed ? 0.1f : -0.1f;
        } else {
            nowSpeed = scrollSpeed;
        }
        nowSpeed = clamp(nowSpeed, 0, 0.5f);
        CDRotation += nowSpeed * delta * 10;
        if (Math.abs(lastScroll - listWidget.getScrollAmount()) <= 3) {
            lastScroll = (float) listWidget.getScrollAmount();
        } else {
            lastScroll += (float) (Math.abs(lastScroll - listWidget.getScrollAmount()) / 10)
                    * (lastScroll < listWidget.getScrollAmount() ? 1 : -1);
        }

        int x = left + 15;
        int y = top + 15;
        int size = 100;
        guiGraphics.pose().translate(x + (float) size / 2, y + (float) size / 2, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(CDRotation));
        guiGraphics.pose().translate(-x - (float) size / 2, -y - (float) size / 2, 0);
        guiGraphics.blit(CD_TEXTURE, x, y, size, size, 0, 0, 256, 256, 256, 256);
        guiGraphics.pose().popPose();
    }

    private void renderPointer(@NotNull GuiGraphics guiGraphics, float delta) {
        if (musicList.size() == listWidget.getSelectedIndex()) {
            if (pointerRotation < 45) {
                pointerRotation += 10f * delta;
                if (pointerRotation > 44) pointerRotation = 45;
            }
        } else {
            if (pointerRotation > 0) {
                pointerRotation -= 10f * delta;
                if (pointerRotation < 1) pointerRotation = 0;
            }
        }
        int x = left + 50;
        int y = top + 3;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 55, y + 3, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(pointerRotation));
        guiGraphics.pose().translate(-x - 55, -y - 3, 0);
        guiGraphics.blit(BACKGROUND_TEXTURE, x, y, 64, 58, 0, 256 - 69, 69, 66, 512, 256);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE && canDelete()) {
            deleteMusic();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP && canMove(true)) {
            moveMusic(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN && canMove(false)) {
            moveMusic(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && super.keyPressed(keyCode, scanCode, modifiers)) {
            sendPackage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void deleteMusic() {
        if (this.listWidget.getSelectedIndex() != musicList.size()) {
            int o = this.listWidget.getSelectedIndex();
            int o1 = o;
            this.musicList.remove(o);
            if (o == this.musicList.size()) o--;
            if (o < 0) o = 0;
            index = o;
            this.clearWidgets();
            this.init();
            listWidget.setSelectedIndex(o);
            this.index = listWidget.getSelectedIndex();
            ModNetwork.CHANNEL.sendToServer(new DeleteMusicDataPacket(o1));
            updateButton();
            sendPackage();
        }
    }

    private void moveMusic(boolean isUp) {
        if (this.listWidget.getSelectedIndex() != musicList.size()) {
            int i1 = listWidget.getSelectedIndex() - (isUp ? 1 : -1);
            ModNetwork.CHANNEL.sendToServer(new MoveMusicDataPacket(listWidget.getSelectedIndex(), i1));
            var l = listWidget.getSelected();
            var l1 = musicList.get(listWidget.getSelectedIndex());
            musicList.set(listWidget.getSelectedIndex(), musicList.get(i1));
            musicList.set(i1, l1);
            listWidget.setEntry(listWidget.getSelectedIndex(), listWidget.children().get(i1));
            listWidget.setEntry(i1, l);
            this.index = i1;
            listWidget.setSelectedIndex(i1);
            updateButton();
            sendPackage();
        }
    }

    private void updateButton() {
        deleteButton.active = canDelete();
        upButton.active = canMove(true);
        downButton.active = canMove(false);
    }

    private boolean canDelete() {
        return this.index != musicList.size();
    }

    private boolean canMove(boolean isUp) {
        if (!canDelete()) return false;
        if (isUp) return this.index > 0;
        return this.index < musicList.size() - 1;
    }

    private void sendPackage() {
        this.index = listWidget.getSelectedIndex();
        ModNetwork.CHANNEL.sendToServer(new MusicListDataPacket(index, this.playModeButton.playMode.ordinal()));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private class MusicListWidget extends ObjectSelectionList<MusicListEntry> {
        public MusicListWidget() {
            super(Minecraft.getInstance(), 197, 153,
                    MusicSelectionScreen.this.top + 12,
                    MusicSelectionScreen.this.top + 128,
                    font.lineHeight + 1);
            x0 = left + 122;
            x1 = backgroundWidth + MusicSelectionScreen.this.left - 5;
            this.setRenderHeader(false, 0);
            this.setRenderTopAndBottom(false);
        }

        @Override
        protected int getScrollbarPosition() {
            return y0 + this.width - 5;
        }

        @Override
        public int getRowWidth() {
            return this.width - 16;
        }

        public void addMusicEntry(String musicName) {
            this.addEntry(new MusicListEntry(musicName));
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int x, int y, float d) {
            super.render(guiGraphics, x, y, d);
            if (this.getMaxScroll() > 0) {
                int l = this.getScrollbarPosition();
                int i1 = (int) ((float) (this.height * this.height) / (float) this.getMaxPosition());
                i1 = Mth.clamp(i1, 32, this.height - 8);
                int k = (int) this.getScrollAmount() * (this.height - i1) / this.getMaxScroll() + y0;
                if (k < this.y0) k = this.y0;
                guiGraphics.blitNineSliced(
                        new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/bar.png"),
                        l - 1, k, 5, i1, 5, 18, 1, 4, 1, 13);
            }
        }

        public int getSelectedIndex() {
            return this.children().indexOf(this.getSelected());
        }

        public void setSelectedIndex(int index) {
            this.setSelected(this.children().get(index));
        }

        public void setEntry(int index, MusicListEntry entry) {
            var l = children();
            l.set(index, entry);
        }
    }

    private class MusicListEntry extends ObjectSelectionList.Entry<MusicListEntry> {
        private final String musicName;

        public MusicListEntry(String musicName) {
            this.musicName = musicName;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(musicName);
        }

        @Override
        public void render(@NotNull GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight,
                           int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (hovered) {
                context.fill(x, y, x + entryWidth - 4, y + entryHeight, 0x80FFFFFF);
            }
            context.drawString(font, font.plainSubstrByWidth(musicName, entryWidth - 10),
                    x + 5, y + (entryHeight - 10) / 2 + 1, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            super.mouseClicked(mouseX, mouseY, button);
            MusicSelectionScreen.this.index = listWidget.children().indexOf(this);
            listWidget.setSelectedIndex(index);
            sendPackage();
            updateButton();
            return true;
        }
    }

    public static void open(List<SongInfo> songs, PlayMode mode, int index) {

        if (index < 0) index = 0;
        if (!songs.isEmpty() && index > songs.size()) {
            index = 0;
        }
        Minecraft.getInstance().setScreen(new MusicSelectionScreen(songs, mode, index));
    }

    public static class PlayModeButton extends Button {
        public PlayMode playMode;

        protected PlayModeButton(int x, int y, OnPress onPress, PlayMode playMode) {
            super(x, y, 22, 22, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.playMode = playMode;
            setTooltip(Tooltip.create(this.playMode.getName()));
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics context, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(context, mouseX, mouseY, partialTick);
            int x = switch (this.playMode) {
                case SEQUENTIAL -> 1;
                case RANDOM -> 26;
                case LOOP -> 51;
            };
            context.blitNineSliced(BUTTON_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                    25, 25, 3, 3, 3, 3);
            context.blit(BACKGROUND_TEXTURE, this.getX(), this.getY(), x, 162, this.width, this.height, 512, 256);
        }
    }

    public static class MoveButton extends Button {
        final boolean isUp;

        protected MoveButton(int x, int y, OnPress onPress, boolean isUp) {
            super(x, y, 22, 22, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.isUp = isUp;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics context, int mouseX, int mouseY, float partialTick) {
            context.blitNineSliced(BUTTON_TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                    4, 25, 25, 0, 0);
            context.blit(BACKGROUND_TEXTURE, this.getX(), this.getY(),
                    isUp ? 76 : 101, 162, this.width, this.height, 512, 256);
        }
    }

    public static class DeleteButton extends Button {
        protected DeleteButton(int x, int y, OnPress onPress) {
            super(x, y, 50, 22, Component.translatable("gui.mengsamanetmusic.play_list.delete"),
                    onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.blitNineSliced(BUTTON_TEXTURE, this.getX(), this.getY(),
                    this.getWidth(), this.getHeight(), 25, 25, 3, 3, 3, 3);
            this.renderString(guiGraphics, Minecraft.getInstance().font, 0xFFFFFFFF);
        }
    }
}
