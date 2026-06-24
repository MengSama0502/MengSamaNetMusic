package com.mengsama.mod.mengsamanetmusic.api;

import com.mengsama.mod.mengsamanetmusic.config.ConfigManager;
import com.mengsama.mod.mengsamanetmusic.network.SyncVipCookiePacket;
import net.neoforged.fml.loading.FMLEnvironment;

public final class VipCookieState {
    private VipCookieState() {
    }

    public static String getClientEffectiveVipCookie() {
        String credCookie = QqCredentialManager.getEffectiveCookie();
        if (credCookie != null && !credCookie.isBlank()) {
            return credCookie;
        }
        return ConfigManager.getQqCookie();
    }

    public static String getServerEffectiveVipCookie() {
        String credCookie = QqCredentialManager.getEffectiveCookie();
        if (credCookie != null && !credCookie.isBlank()) {
            return credCookie;
        }
        return ConfigManager.getQqCookie();
    }

    public static boolean canSkipVipCookieWarningOnClient() {
        String local = getClientEffectiveVipCookie();
        if (local != null && !local.isBlank()) {
            return true;
        }
        return SyncVipCookiePacket.CLIENT_HAS_VIP_COOKIE;
    }

    public static boolean hasServerVipCookieAvailable() {
        String cookie = getServerEffectiveVipCookie();
        return cookie != null && !cookie.isBlank();
    }

    public static String getEffectiveVipCookie() {
        if (ConfigManager.isDedicatedServer()) {
            return getServerEffectiveVipCookie();
        }
        return getClientEffectiveVipCookie();
    }
}
