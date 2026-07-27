package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.QqCredentialManager;
import com.mengsama.mod.mengsamanetmusic.api.QqLoginService;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

public class QqLoginScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation qrLocation = new ResourceLocation(MengSamaNetMusic.MOD_ID, "qq_login_qr");
    private DynamicTexture qrTexture;
    private volatile QqLoginService.LoginState state = QqLoginService.LoginState.IDLE;
    private volatile boolean polling;
    private long nextPollAt;

    public QqLoginScreen(Screen parent) {
        super(Component.translatable("gui.mengsamanetmusic.qq_login.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.mengsamanetmusic.qq_login.refresh"), b -> fetchQr())
                .bounds(width / 2 - 122, height / 2 + 82, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mengsamanetmusic.qq_login.logout"), b -> {
                    QqCredentialManager.clear();
                    state = QqLoginService.LoginState.IDLE;
                    polling = false;
                }).bounds(width / 2 - 38, height / 2 + 82, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.mengsamanetmusic.back"), b -> onClose())
                .bounds(width / 2 + 46, height / 2 + 82, 76, 20).build());
        if (QqCredentialManager.hasValidCredential()) {
            state = QqLoginService.LoginState.SUCCESS;
        } else {
            fetchQr();
        }
    }

    private void fetchQr() {
        polling = false;
        state = QqLoginService.LoginState.FETCHING_QR;
        QqLoginService.QrSession.reset();
        QqLoginService.fetchQrCode().thenAccept(bytes -> Minecraft.getInstance().execute(() -> {
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
                if (qrTexture != null) qrTexture.close();
                qrTexture = new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(qrLocation, qrTexture);
                state = QqLoginService.LoginState.WAITING_SCAN;
                polling = true;
                nextPollAt = Util.getMillis() + 1000;
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to decode QQ QR code", e);
                state = QqLoginService.LoginState.FAILED;
            }
        })).exceptionally(error -> {
            state = QqLoginService.LoginState.FAILED;
            return null;
        });
    }

    @Override
    public void tick() {
        if (!polling || Util.getMillis() < nextPollAt) return;
        polling = false;
        CompletableFuture<QqLoginService.LoginState> future = QqLoginService.pollLogin();
        future.thenAccept(result -> Minecraft.getInstance().execute(() -> {
            state = result;
            if (result == QqLoginService.LoginState.SUCCESS) {
                // Login service already published the shared in-memory session; avoid a redundant disk reload.
                polling = false;
            } else if (result != QqLoginService.LoginState.FAILED && result != QqLoginService.LoginState.QR_EXPIRED) {
                polling = true;
                nextPollAt = Util.getMillis() + 1800;
            }
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 100, 0xFFFFFFFF);
        if (qrTexture != null) {
            graphics.blit(qrLocation, width / 2 - 72, height / 2 - 76, 0, 0, 144, 144, 144, 144);
        }
        graphics.drawCenteredString(font, stateText(), width / 2, height / 2 + 72, state == QqLoginService.LoginState.SUCCESS ? 0xFF55FF55 : 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component stateText() {
        String suffix = switch (state) {
            case FETCHING_QR -> "loading";
            case WAITING_SCAN -> "waiting";
            case AUTHORIZING, LOGGING_IN -> "authorizing";
            case SUCCESS -> "success";
            case QR_EXPIRED -> "expired";
            case FAILED -> "failed";
            default -> "idle";
        };
        return Component.translatable("gui.mengsamanetmusic.qq_login." + suffix);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        polling = false;
        QqLoginService.QrSession.reset();
        if (qrTexture != null) {
            Minecraft.getInstance().getTextureManager().release(qrLocation);
            qrTexture.close();
            qrTexture = null;
        }
    }
}
