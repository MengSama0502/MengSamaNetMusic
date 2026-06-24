package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.cache.CacheManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.List;

public class CacheManagerScreen extends Screen {
    private static final DecimalFormat PROGRESS_FORMAT = new DecimalFormat("0.0%");
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("0.00");

    private int refreshTicks = 0;
    private static final int REFRESH_INTERVAL = 5;

    public CacheManagerScreen() {
        super(Component.literal("\u4E0B\u8F7D\u76D1\u63A7"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        guiGraphics.drawCenteredString(this.font, "\u4E0B\u8F7D\u4EFB\u52A1\u76D1\u63A7", this.width / 2, 20, 0xFFFFFF);

        List<CacheManager.FileDownloadThread> downloads = CacheManager.getThreads();

        String countText = "\u6D3B\u8DC3\u4E0B\u8F7D\u4EFB\u52A1: " + downloads.size();
        guiGraphics.drawString(this.font, countText, 10, 50, 0xFFFFFF);

        int startY = 80;
        int lineHeight = 30;

        if (downloads.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "\u6CA1\u6709\u6D3B\u8DC3\u7684\u4E0B\u8F7D\u4EFB\u52A1", this.width / 2, startY, 0x888888);
        } else {
            for (int i = 0; i < downloads.size(); i++) {
                CacheManager.FileDownloadThread thread = downloads.get(i);
                renderDownloadEntry(guiGraphics, thread, 10, startY + i * lineHeight, this.width - 20);
            }
        }

        poseStack.popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderDownloadEntry(GuiGraphics guiGraphics, CacheManager.FileDownloadThread thread, int x, int y, int width) {
        String threadInfo = String.format("ID: %s | \u8D44\u6E90: %d | \u7C7B\u578B: %s",
                thread.getThreadId(), thread.getResourceId(), thread.getFileType());
        guiGraphics.drawString(this.font, threadInfo, x, y, 0xFFFFFF);

        float progress = thread.getProgress();
        long downloaded = thread.getDownloadedBytes();
        long total = thread.getTotalBytes();

        String progressText;
        if (total > 0) {
            String downloadedStr = formatFileSize(downloaded);
            String totalStr = formatFileSize(total);
            progressText = String.format("%s / %s (%s)", downloadedStr, totalStr, PROGRESS_FORMAT.format(progress));
        } else {
            progressText = String.format("%s / \u672A\u77E5\u5927\u5C0F (%s)", formatFileSize(downloaded), PROGRESS_FORMAT.format(progress));
        }

        guiGraphics.drawString(this.font, progressText, x, y + 10, 0xCCCCCC);

        int progressBarWidth = width - 20;
        int progressBarHeight = 6;
        int progressBarX = x;
        int progressBarY = y + 20;

        guiGraphics.fill(progressBarX, progressBarY,
                progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFF555555);

        int progressWidth = (int) (progressBarWidth * progress);
        int color = getProgressBarColor(thread);
        guiGraphics.fill(progressBarX, progressBarY,
                progressBarX + progressWidth, progressBarY + progressBarHeight, color);

        String statusText = getStatusText(thread);
        int statusColor = getStatusColor(thread);
        guiGraphics.drawString(this.font, statusText, x + progressBarWidth + 5, progressBarY, statusColor);
    }

    private String getStatusText(CacheManager.FileDownloadThread thread) {
        if (thread.isCompleted()) return "\u5B8C\u6210";
        if (thread.isFailed()) return "\u5931\u8D25: " + thread.getErrorMessage();
        if (thread.isInterrupted()) return "\u5DF2\u4E2D\u65AD";
        return "\u4E0B\u8F7D\u4E2D...";
    }

    private int getStatusColor(CacheManager.FileDownloadThread thread) {
        if (thread.isCompleted()) return 0xFF00FF00;
        if (thread.isFailed()) return 0xFFFF0000;
        if (thread.isInterrupted()) return 0xFFFFA500;
        return 0xFFFFFF00;
    }

    private int getProgressBarColor(CacheManager.FileDownloadThread thread) {
        if (thread.isCompleted()) return 0xFF00FF00;
        if (thread.isFailed()) return 0xFFFF0000;
        if (thread.isInterrupted()) return 0xFFFFA500;
        return 0xFF0000FF;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        } else {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        }
    }

    @Override
    public void tick() {
        super.tick();
        refreshTicks++;
        if (refreshTicks >= REFRESH_INTERVAL) {
            refreshTicks = 0;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
