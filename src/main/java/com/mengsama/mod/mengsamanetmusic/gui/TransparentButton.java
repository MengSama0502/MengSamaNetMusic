package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.config.MusicPlayerUiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** A text-only button: transparent at rest, outlined when hovered, selected or layout-edited. */
public final class TransparentButton extends AbstractButton {
    private final OnPress onPress;
    private boolean selected;
    private boolean editing;
    private Integer outlineColor;

    public TransparentButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    public static Builder builder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public void setSelected(boolean selected) { this.selected = selected; }
    public void setEditing(boolean editing) { this.editing = editing; }
    public void setOutlineColor(int color) { this.outlineColor = color; }

    @Override
    public void onPress() { this.onPress.onPress(this); }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        MusicPlayerUiConfig.Values theme = MusicPlayerUiConfig.get();
        int color = this.active ? theme.primaryText() : theme.secondaryText();
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2,
                this.getY() + (this.height - 8) / 2, color);
        if (this.isHoveredOrFocused() || this.selected || this.editing) {
            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height,
                    this.selected ? theme.secondaryAccent() : this.outlineColor == null ? theme.accent() : this.outlineColor);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    public interface OnPress { void onPress(TransparentButton button); }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x, y, width = 100, height = 20;

        private Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) { this.x = x; this.y = y; return this; }
        public Builder size(int width, int height) { this.width = width; this.height = height; return this; }
        public TransparentButton build() { return new TransparentButton(x, y, width, height, message, onPress); }
    }
}
