package com.mengsama.mod.mengsamanetmusic.gui;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.gui.widget.QRCodeImage;
import com.mengsama.mod.mengsamanetmusic.gui.widget.RadioWidget;
import com.mengsama.mod.mengsamanetmusic.gui.widget.SupplierChildLinearLayout;
import com.mengsama.mod.mengsamanetmusic.util.RenderUtil;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LoginScreen extends Screen {
    private static final ResourceLocation REFRESH = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/refresh.png");
    private static final ResourceLocation CHECK = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/gui/check.png");
    private static final Gson GSON = new Gson();

    private final LinearLayout rootLayout;
    private final QRCodeImage qrCodeImage;
    private final EditBox usernameInput;
    private final EditBox passwordInput;
    private final Button sendCaptchaButton;
    private final Button loginButton;
    private Component tip;
    private LoginMethod loginMethod = LoginMethod.EMAIL;
    private QRState qrState = QRState.PRELOAD;
    private String unikey = "";
    private int timer = 0;

    public LoginScreen() {
        super(Component.literal("Login"));
        rootLayout = new LinearLayout(getCenterX(390), getCenterY(150), 390, 150, LinearLayout.Orientation.HORIZONTAL);

        qrCodeImage = new QRCodeImage(0, 0, 150, 150, (x, y) -> refreshQRCode());
        qrCodeImage.setOnRender((guiGraphics, mouseX, mouseY, partialTick) -> {
            QRState state = getQrState();
            if (state.equals(QRState.ERROR) || state.equals(QRState.EXPIRE) || state.equals(QRState.SCAN_SUCCESS)) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 200);
                guiGraphics.fill(
                        qrCodeImage.getX(), qrCodeImage.getY(),
                        qrCodeImage.getX() + qrCodeImage.getWidth(),
                        qrCodeImage.getY() + qrCodeImage.getHeight(),
                        0xE5FFFFFF
                );
                Component tip = switch (state) {
                    case EXPIRE ->
                            Component.translatable("gui.mengsamanetmusic.login.qr_code.tip.expire").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
                    case SCAN_SUCCESS ->
                            Component.translatable("gui.mengsamanetmusic.login.qr_code.tip.scan_success").withStyle(ChatFormatting.DARK_GREEN).withStyle(ChatFormatting.BOLD);
                    case ERROR ->
                            Component.translatable("gui.mengsamanetmusic.login.qr_code.tip.error").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
                    default -> null;
                };
                RenderUtil.drawCenteredStringNoShadow(guiGraphics, font, tip,
                        qrCodeImage.getX() + qrCodeImage.getWidth() / 2, qrCodeImage.getY() + 35,
                        tip.getStyle().getColor().getValue());
                Component actionTip;
                int imageWidth = 40;
                if (!state.equals(QRState.SCAN_SUCCESS)) {
                    actionTip = Component.translatable("gui.mengsamanetmusic.login.qr_code.tip.reload").withStyle(ChatFormatting.BLACK).withStyle(ChatFormatting.BOLD);
                    guiGraphics.blit(REFRESH, qrCodeImage.getX() + qrCodeImage.getWidth() / 2 - imageWidth / 2,
                            qrCodeImage.getY() + 70, 0, 0, imageWidth, imageWidth, imageWidth, imageWidth);
                } else {
                    actionTip = Component.translatable("gui.mengsamanetmusic.login.qr_code.tip.confirm").withStyle(ChatFormatting.BLACK).withStyle(ChatFormatting.BOLD);
                    guiGraphics.blit(CHECK, qrCodeImage.getX() + qrCodeImage.getWidth() / 2 - imageWidth / 2,
                            qrCodeImage.getY() + 70, 0, 0, imageWidth, imageWidth, imageWidth, imageWidth);
                }
                int actionTipHeight = qrCodeImage.getY() + 50;
                RenderUtil.drawCenteredStringNoShadow(guiGraphics, font, actionTip,
                        qrCodeImage.getX() + qrCodeImage.getWidth() / 2, actionTipHeight,
                        actionTip.getStyle().getColor().getValue());
                guiGraphics.pose().popPose();
            }
        });
        rootLayout.addChild(qrCodeImage);
        addRenderableWidget(qrCodeImage);

        rootLayout.addChild(new Button.Builder(Component.empty(), button -> {}).size(40, 150).build());

        LinearLayout inputLayout = new LinearLayout(200, 130, LinearLayout.Orientation.VERTICAL);
        rootLayout.addChild(inputLayout, LayoutSettings.defaults().paddingVertical(10));

        List<Component> options = List.of(
                Component.translatable("gui.mengsamanetmusic.login_type.email"),
                Component.translatable("gui.mengsamanetmusic.login_type.phone")
        );
        RadioWidget loginMethodSwitch = new RadioWidget(0, 0, options, 10, 0, this::switchLoginMethod);
        inputLayout.addChild(loginMethodSwitch, inputLayout.newChildLayoutSettings().alignHorizontallyCenter());
        addRenderableWidget(loginMethodSwitch);

        usernameInput = new EditBox(getFont(), 0, 0, 198, 20, Component.literal("Username Input"));
        inputLayout.addChild(usernameInput, LayoutSettings.defaults().padding(1));
        addRenderableWidget(usernameInput);

        passwordInput = new EditBox(getFont(), 0, 0, 198, 20, Component.literal("Password Input"));
        addRenderableWidget(passwordInput);

        sendCaptchaButton = new Button.Builder(
                Component.translatable("gui.mengsamanetmusic.login.send_captcha"), this::sendCaptcha)
                .size(60, 22).build();
        addRenderableWidget(sendCaptchaButton);

        List<SupplierChildLinearLayout.ChildContainer> emailChilds = List.of(
                new SupplierChildLinearLayout.ChildContainer(passwordInput, LayoutSettings.defaults().paddingTop(1).paddingHorizontal(1))
        );
        List<SupplierChildLinearLayout.ChildContainer> phoneChilds = List.of(
                new SupplierChildLinearLayout.ChildContainer(passwordInput, LayoutSettings.defaults().paddingRight(5).paddingTop(1).paddingLeft(1)),
                new SupplierChildLinearLayout.ChildContainer(sendCaptchaButton, LayoutSettings.defaults())
        );
        SupplierChildLinearLayout passwordLayout = new SupplierChildLinearLayout(
                200, 22, SupplierChildLinearLayout.Orientation.HORIZONTAL,
                () -> switch (loginMethod) {
                    case EMAIL -> emailChilds;
                    case PHONE -> phoneChilds;
                });
        inputLayout.addChild(passwordLayout);

        loginButton = new Button.Builder(Component.translatable("gui.mengsamanetmusic.login.login"), this::login)
                .width(200).build();
        inputLayout.addChild(loginButton);
        addRenderableWidget(loginButton);

        loginMethodSwitch.setSelected(0);
        refreshQRCode();
    }

    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    protected void init() {
        rootLayout.setPosition(getCenterX(390), getCenterY(150));
        rootLayout.arrangeElements();
    }

    @Override
    public void tick() {
        this.usernameInput.tick();
        this.passwordInput.tick();
        QRState state = getQrState();
        if ((state.equals(QRState.WAIT) || state.equals(QRState.SCAN_SUCCESS)) && this.timer == 0) {
            checkQRLoginStatus();
        }
        this.timer = (this.timer + 1) % 40;
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
        init();
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

    public void setQrState(QRState qrState) {
        this.qrState = qrState;
    }

    public QRState getQrState() {
        return qrState;
    }

    private void refreshQRCode() {
        QRState state = getQrState();
        if (state.equals(QRState.LOADING) || state.equals(QRState.WAIT) || state.equals(QRState.SCAN_SUCCESS)) {
            return;
        }
        setQrState(QRState.LOADING);
        this.unikey = "";
        CompletableFuture.supplyAsync(() -> {
            try {
                String resp = callNeteaseApi("getQRKey");
                Map<String, Object> qrKey = GSON.fromJson(resp, Map.class);
                double code = ((Number) qrKey.get("code")).doubleValue();
                if (code != 200) {
                    throw new RuntimeException("Failed to get QR key, response code: " + code);
                }
                return (String) qrKey.get("unikey");
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to get QR key", e);
                setQrState(QRState.ERROR);
                throw new RuntimeException(e);
            }
        }, Util.backgroundExecutor()).thenAccept(unikey -> {
            qrCodeImage.updateQRCode(String.format("https://music.163.com/login?codekey=%s", unikey));
            this.unikey = unikey;
            if (qrCodeImage.getLoadingState().equals(com.mengsama.mod.mengsamanetmusic.gui.widget.DynamicImage.LoadingState.ERROR)) {
                setQrState(QRState.ERROR);
            } else {
                setQrState(QRState.WAIT);
            }
        });
    }

    private void switchLoginMethod(int i) {
        if (i == 0) {
            this.loginMethod = LoginMethod.EMAIL;
            this.sendCaptchaButton.visible = false;
            this.passwordInput.setWidth(198);
            this.passwordInput.setFormatter((value, displayPos) -> FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
        } else if (i == 1) {
            this.loginMethod = LoginMethod.PHONE;
            this.sendCaptchaButton.visible = true;
            this.passwordInput.setWidth(134);
            this.passwordInput.setFormatter((value, displayPos) -> FormattedCharSequence.forward(value, Style.EMPTY));
        }
        this.passwordInput.setValue("");
        this.tip = null;
        rootLayout.arrangeElements();
    }

    private void sendCaptcha(Button button) {
        if (usernameInput.getValue().isBlank()) {
            tip = Component.translatable("gui.mengsamanetmusic.login.tip.input_empty").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
            return;
        }
        tip = null;
        CompletableFuture.runAsync(() -> {
            try {
                String response = callNeteaseApi("sendCaptcha", usernameInput.getValue());
                Map<String, Object> result = GSON.fromJson(response, Map.class);
                double code = ((Number) result.get("code")).doubleValue();
                if (code != 200) {
                    String message = Optional.ofNullable((String) result.get("message")).orElse("unknown error");
                    tip = Component.translatable("gui.mengsamanetmusic.login.tip.send_captcha.error", Component.literal(message))
                            .withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
                    return;
                }
                tip = Component.translatable("gui.mengsamanetmusic.login.tip.send_captcha.success").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD);
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to send captcha", e);
            }
        }, Util.backgroundExecutor());
    }

    private void login(Button button) {
        if (usernameInput.getValue().isBlank() || passwordInput.getValue().isBlank()) {
            tip = Component.translatable("gui.mengsamanetmusic.login.tip.input_empty").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
            return;
        }
        tip = null;
        CompletableFuture.runAsync(() -> {
            try {
                String response;
                if (loginMethod == LoginMethod.EMAIL) {
                    response = callNeteaseApi("emailLogin", usernameInput.getValue(), passwordInput.getValue());
                } else {
                    response = callNeteaseApi("phoneCaptchaLogin", usernameInput.getValue(), passwordInput.getValue());
                }
                Map<String, Object> result = GSON.fromJson(response, Map.class);
                double code = ((Number) result.get("code")).doubleValue();
                if (code != 200) {
                    String message = Optional.ofNullable((String) result.get("message")).orElse("unknown error");
                    tip = Component.translatable("gui.mengsamanetmusic.login.tip.error", Component.literal(message))
                            .withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
                    return;
                }
                this.success();
            } catch (Exception e) {
                MengSamaNetMusic.LOGGER.error("Failed to login", e);
                tip = Component.translatable("gui.mengsamanetmusic.login.tip.error",
                        Component.literal(e.getMessage())).withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
            }
        }, Util.backgroundExecutor());
    }

    private void checkQRLoginStatus() {
        if (!this.unikey.isEmpty()) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return callNeteaseApi("checkQRLoginStatus", this.unikey);
                } catch (Exception e) {
                    MengSamaNetMusic.LOGGER.error("Failed to check QR login status", e);
                    throw new RuntimeException(e);
                }
            }, Util.backgroundExecutor()).thenAccept(response -> {
                Map<String, Object> status = GSON.fromJson(response, Map.class);
                double code = ((Number) status.get("code")).doubleValue();
                switch ((int) code) {
                    case 800 -> setQrState(QRState.EXPIRE);
                    case 801 -> setQrState(QRState.WAIT);
                    case 802 -> setQrState(QRState.SCAN_SUCCESS);
                    case 803 -> this.success();
                }
            });
        }
    }

    public void success() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.submit(() -> minecraft.setScreen(new LoginSuccessScreen()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key mouseKey = InputConstants.getKey(keyCode, scanCode);
        if (this.getMinecraft().options.keyInventory.isActiveAndMatches(mouseKey)
                && (usernameInput.isFocused() || passwordInput.isFocused())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        MutableComponent usernameTip = Component.empty();
        MutableComponent passwordTip = Component.empty();
        if (loginMethod.equals(LoginMethod.EMAIL)) {
            usernameTip = Component.translatable("gui.mengsamanetmusic.login.email.tip");
            passwordTip = Component.translatable("gui.mengsamanetmusic.login.password.tip");
        } else if (loginMethod.equals(LoginMethod.PHONE)) {
            usernameTip = Component.translatable("gui.mengsamanetmusic.login.phone.tip");
            passwordTip = Component.translatable("gui.mengsamanetmusic.login.captcha.tip");
        }
        renderTipInEditBox(pGuiGraphics, usernameTip.withStyle(ChatFormatting.ITALIC), usernameInput);
        renderTipInEditBox(pGuiGraphics, passwordTip.withStyle(ChatFormatting.ITALIC), passwordInput);

        if (tip != null) {
            pGuiGraphics.drawString(font, tip, loginButton.getX(), loginButton.getY() - 13,
                    tip.getStyle().getColor().getValue(), false);
        }
    }

    public void renderTipInEditBox(GuiGraphics guiGraphics, Component text, EditBox editBox) {
        if (Util.isBlank(editBox.getValue()) && editBox.isVisible() && !editBox.isFocused()) {
            guiGraphics.drawString(font, text, editBox.getX() + 4,
                    editBox.getY() + (editBox.getHeight() - font.lineHeight) / 2 + 1, 0xFFAAAAAA);
        }
    }

    @Override
    public void removed() {
        qrCodeImage.close();
    }

    private static String callNeteaseApi(String method, Object... args) throws Exception {
        Class<?> apiClass = Class.forName("com.mengsama.mod.mengsamanetmusic.api.BetterNetWorker");
        Object neteaseApi = Class.forName("com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic")
                .getField("NET_EASE_API").get(null);

        java.lang.reflect.Method apiMethod;
        if ("emailLogin".equals(method)) {
            apiMethod = neteaseApi.getClass().getMethod("emailLogin", String.class, String.class);
            Object httpResponse = apiMethod.invoke(neteaseApi, args);
            return (String) httpResponse.getClass().getField("body").get(httpResponse);
        } else if ("phoneCaptchaLogin".equals(method)) {
            apiMethod = neteaseApi.getClass().getMethod("phoneCaptchaLogin", String.class, String.class);
            Object httpResponse = apiMethod.invoke(neteaseApi, args);
            return (String) httpResponse.getClass().getField("body").get(httpResponse);
        } else if ("getQRKey".equals(method)) {
            apiMethod = neteaseApi.getClass().getMethod("getQRKey");
            return (String) apiMethod.invoke(neteaseApi);
        } else if ("sendCaptcha".equals(method)) {
            apiMethod = neteaseApi.getClass().getMethod("sendCaptcha", String.class);
            return (String) apiMethod.invoke(neteaseApi, args);
        } else if ("checkQRLoginStatus".equals(method)) {
            apiMethod = neteaseApi.getClass().getMethod("checkQRLoginStatus", String.class);
            Object httpResponse = apiMethod.invoke(neteaseApi, args);
            return (String) httpResponse.getClass().getField("body").get(httpResponse);
        }
        throw new IllegalArgumentException("Unknown API method: " + method);
    }

    public enum QRState {
        PRELOAD, LOADING, ERROR, WAIT, EXPIRE, SCAN_SUCCESS
    }

    public enum LoginMethod {
        EMAIL, PHONE
    }
}
