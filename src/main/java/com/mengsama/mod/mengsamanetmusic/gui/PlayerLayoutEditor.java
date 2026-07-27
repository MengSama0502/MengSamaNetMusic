package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.config.MusicPlayerUiConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-only editor shared by portable, normal block and portable-block player layouts. */
public final class PlayerLayoutEditor {
    private final String screenType;
    private final Map<String, AbstractWidget> widgets = new LinkedHashMap<>();
    private int originX, originY, panelWidth, panelHeight;
    private boolean editing, dragging;
    private String selectedId;
    private double dragOffsetX, dragOffsetY;
    private Component notice = Component.empty();

    public PlayerLayoutEditor(String screenType) { this.screenType = screenType; }

    public void begin(int originX, int originY, int panelWidth, int panelHeight) {
        this.originX = originX;
        this.originY = originY;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.widgets.clear();
        this.selectedId = null;
        this.dragging = false;
    }

    public <T extends AbstractWidget> T register(String id, T widget, int defaultRelativeX, int defaultRelativeY) {
        MusicPlayerUiConfig.Position p = MusicPlayerUiConfig.get().position(screenType, id,
                defaultRelativeX, defaultRelativeY, widget.getWidth(), widget.getHeight());
        widget.setX(originX + p.x);
        widget.setY(originY + p.y);
        if (widget instanceof TransparentButton button) button.setEditing(editing);
        widgets.put(id, widget);
        return widget;
    }

    public boolean keyPressed(int keyCode, int modifiers, Runnable reinitialize) {
        if (keyCode == GLFW.GLFW_KEY_F6) {
            editing = !editing;
            dragging = false;
            notice = Component.literal(editing ? "已进入布局编辑" : "已退出布局编辑");
            for (AbstractWidget widget : widgets.values())
                if (widget instanceof TransparentButton button) button.setEditing(editing);
            return true;
        }
        if (!editing) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editing = false;
            dragging = false;
            for (AbstractWidget widget : widgets.values())
                if (widget instanceof TransparentButton button) button.setEditing(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S) {
            capture();
            try {
                MusicPlayerUiConfig.save();
                notice = Component.literal("布局已保存到 music_player_ui.json");
            } catch (IOException e) {
                notice = Component.literal("布局保存失败，请查看日志");
                MengSamaNetMusic.LOGGER.error("Failed to save music player UI layout", e);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            try {
                MusicPlayerUiConfig.reset();
                notice = Component.literal("已恢复默认布局");
                reinitialize.run();
            } catch (IOException e) {
                notice = Component.literal("默认布局恢复失败，请查看日志");
                MengSamaNetMusic.LOGGER.error("Failed to reset music player UI layout", e);
            }
            return true;
        }
        int dx = 0, dy = 0;
        if (keyCode == GLFW.GLFW_KEY_LEFT) dx = -1;
        else if (keyCode == GLFW.GLFW_KEY_RIGHT) dx = 1;
        else if (keyCode == GLFW.GLFW_KEY_UP) dy = -1;
        else if (keyCode == GLFW.GLFW_KEY_DOWN) dy = 1;
        if ((dx != 0 || dy != 0) && selectedId != null) {
            int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            moveSelected(dx * step, dy * step);
            return true;
        }
        return true; // Editing consumes keys so controls cannot accidentally activate.
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!editing || button != 0) return false;
        AbstractWidget hit = null;
        String hitId = null;
        for (Map.Entry<String, AbstractWidget> entry : widgets.entrySet()) {
            AbstractWidget w = entry.getValue();
            if (w.visible && mouseX >= w.getX() && mouseX < w.getX() + w.getWidth()
                    && mouseY >= w.getY() && mouseY < w.getY() + w.getHeight()) {
                hit = w; hitId = entry.getKey();
            }
        }
        select(hitId);
        if (hit != null) {
            dragging = true;
            dragOffsetX = mouseX - hit.getX();
            dragOffsetY = mouseY - hit.getY();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!editing || !dragging || button != 0 || selectedId == null) return false;
        AbstractWidget widget = widgets.get(selectedId);
        if (widget == null) return true;
        setClamped(widget, (int)Math.round(mouseX - dragOffsetX) - originX,
                (int)Math.round(mouseY - dragOffsetY) - originY);
        return true;
    }

    public boolean mouseReleased(int button) {
        if (!editing || button != 0) return false;
        dragging = false;
        return true;
    }

    public void render(GuiGraphics graphics) {
        if (!editing) return;
        for (AbstractWidget widget : widgets.values())
            graphics.renderOutline(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                    widget == widgets.get(selectedId) ? 0xFF4ECDC4 : 0xAA7C6FFF);
        int center = originX + panelWidth / 2;
        graphics.fill(originX + 6, originY + 2, originX + panelWidth - 6, originY + 23, 0xCC101020);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                "布局编辑：点击选择 / 拖动 / 方向键微调 / Shift+方向键 10px", center, originY + 5, 0xFFFFFFFF);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                "S 保存  R 重置  F6或Esc退出", center, originY + 14, 0xFF4ECDC4);
        if (!notice.getString().isEmpty())
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, notice,
                    center, originY + panelHeight - 12, 0xFFFFFFFF);
    }

    private void select(String id) {
        selectedId = id;
        for (Map.Entry<String, AbstractWidget> entry : widgets.entrySet())
            if (entry.getValue() instanceof TransparentButton button) button.setSelected(entry.getKey().equals(id));
    }

    private void moveSelected(int dx, int dy) {
        AbstractWidget widget = widgets.get(selectedId);
        if (widget != null) setClamped(widget, widget.getX() - originX + dx, widget.getY() - originY + dy);
    }

    private void setClamped(AbstractWidget widget, int relativeX, int relativeY) {
        int x = Math.max(0, Math.min(panelWidth - widget.getWidth(), relativeX));
        int y = Math.max(0, Math.min(panelHeight - widget.getHeight(), relativeY));
        widget.setX(originX + x);
        widget.setY(originY + y);
        MusicPlayerUiConfig.get().setPosition(screenType, selectedId, x, y, widget.getWidth(), widget.getHeight());
    }

    private void capture() {
        for (Map.Entry<String, AbstractWidget> entry : widgets.entrySet()) {
            AbstractWidget w = entry.getValue();
            MusicPlayerUiConfig.get().setPosition(screenType, entry.getKey(), w.getX() - originX, w.getY() - originY,
                    w.getWidth(), w.getHeight());
        }
    }
}
