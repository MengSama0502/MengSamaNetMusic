package com.mengsama.mod.mengsamanetmusic.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LoginSuccessScreen extends Screen {

    private final LinearLayout rootLayout;
    private final Button checkImage;
    private final Component text = Component.translatable("gui.mengsamanetmusic.login.success");
    private final Button loginSuccessText;

    public LoginSuccessScreen() {
        super(Component.empty());
        rootLayout = new LinearLayout(getCenterX(120), getCenterY(100), 120, 150, LinearLayout.Orientation.VERTICAL);

        checkImage = Button.builder(Component.empty(), b -> {}).size(50, 50).build();
        rootLayout.addChild(checkImage, rootLayout.newChildLayoutSettings().alignHorizontallyCenter());

        Font font = Minecraft.getInstance().font;
        loginSuccessText = Button.builder(Component.empty(), b -> {}).size(font.width(text), font.lineHeight).build();
        rootLayout.addChild(loginSuccessText, rootLayout.newChildLayoutSettings().paddingTop(10).alignHorizontallyCenter());

        Button confirm = Button.builder(Component.translatable("gui.mengsamanetmusic.login.confirm"), b -> this.onClose())
                .size(120, 20).build();
        rootLayout.addChild(confirm, rootLayout.newChildLayoutSettings().paddingTop(50).alignHorizontallyCenter());
        addRenderableWidget(confirm);
    }

    @Override
    protected void init() {
        rootLayout.setPosition(getCenterX(120), getCenterY(100));
        rootLayout.arrangeElements();
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
        init();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderBackground(pGuiGraphics);
        drawCheckIcon(pGuiGraphics, checkImage.getX(), checkImage.getY(), 50);
        pGuiGraphics.drawString(font, text, loginSuccessText.getX(), loginSuccessText.getY(), ChatFormatting.GREEN.getColor());
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    private static void drawCheckIcon(GuiGraphics graphics, int x, int y, int size) {
        int thickness = Math.max(2, size / 10);
        int color = 0xFF2E7D32;
        for (int i = 0; i < size / 4; i++) {
            graphics.fill(x + size / 5 + i, y + size / 2 + i - thickness,
                    x + size / 5 + i + thickness, y + size / 2 + i + thickness, color);
        }
        for (int i = 0; i < size / 2; i++) {
            graphics.fill(x + size / 3 + i, y + size / 2 + size / 4 - i - thickness,
                    x + size / 3 + i + thickness, y + size / 2 + size / 4 - i + thickness, color);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private int getCenter(int length, int eleLength) {
        return length / 2 - eleLength / 2;
    }

    private int getCenterX(int eleLength) {
        return getCenter(this.width, eleLength);
    }

    private int getCenterY(int eleLength) {
        return getCenter(this.height, eleLength);
    }
}
