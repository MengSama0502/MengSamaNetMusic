package com.mengsama.mod.mengsamanetmusic.gui.widget;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.util.QRUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class QRCodeImage extends DynamicImage {
    protected BiConsumer<Double, Double> onClick;
    protected RenderConsumer onRender;

    public QRCodeImage(int pX, int pY, int pWidth, int pHeight, BiConsumer<Double, Double> onClick) {
        super(pX, pY, pWidth, pHeight, new ResourceLocation(MengSamaNetMusic.MOD_ID, "qr_code" + System.nanoTime()), state -> switch (state) {
            case LOADING -> Component.translatable("gui.mengsamanetmusic.qr_code.loading");
            case ERROR -> Component.translatable("gui.mengsamanetmusic.qr_code.load_failed");
            default -> Component.empty();
        });
        this.onClick = onClick;
    }

    public void setOnRender(RenderConsumer onRender) {
        this.onRender = onRender;
    }

    public void updateQRCode(String content) {
        try {
            setState(LoadingState.LOADING);
            setImage(QRUtil.generateQRCode(content));
            setState(LoadingState.LOADED);
        } catch (Exception e) {
            MengSamaNetMusic.LOGGER.error("Failed to generate QR code image", e);
            setState(LoadingState.ERROR);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(guiGraphics, pMouseX, pMouseY, pPartialTick);
        if (onRender != null) {
            onRender.accept(guiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        if (onClick != null) {
            onClick.accept(pMouseX, pMouseY);
        }
    }
}
