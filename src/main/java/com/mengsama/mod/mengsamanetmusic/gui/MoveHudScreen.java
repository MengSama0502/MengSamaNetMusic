package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.hud.MusicInfoHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MoveHudScreen extends Screen {
    public int x;
    public int y;
    private final Screen parent;

    public MoveHudScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.mengsamanetmusic.hud.reset"), button -> {
                            x = 10;
                            y = 10;
                        })
                .pos(width - 170, height - 30)
                .size(50, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.mengsamanetmusic.hud.close"),
                        button -> onClose())
                .pos(width - 60, height - 30)
                .size(50, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.mengsamanetmusic.hud.not_save"),
                        button -> Minecraft.getInstance().setScreen(parent))
                .pos(width - 115, height - 30)
                .size(50, 20)
                .build());

        x = 10;
        y = 10;

        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            Object hudX = configClass.getField("hudX").get(null);
            Object hudY = configClass.getField("hudY").get(null);
            if (hudX instanceof Integer) x = (int) hudX;
            if (hudY instanceof Integer) y = (int) hudY;
        } catch (Exception ignored) {
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(x, y, x + 100, y + 40, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("config.mengsamanetmusic.hud.text"),
                x + 50, y + 20 - Minecraft.getInstance().font.lineHeight / 2, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("config.mengsamanetmusic.hud.title"),
                width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        x = (int) mouseX - 50;
        y = (int) mouseY - 20;
        return true;
    }

    @Override
    public void onClose() {

        try {
            Class<?> configClass = Class.forName("com.mengsama.mod.mengsamanetmusic.config.ClientConfig");
            configClass.getField("hudX").set(null, x);
            configClass.getField("hudY").set(null, y);

            try {
                configClass.getMethod("saveConfig").invoke(null);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        Minecraft.getInstance().setScreen(parent);
        MusicInfoHud.setPos(x, y);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new MoveHudScreen(Minecraft.getInstance().screen));
    }
}
