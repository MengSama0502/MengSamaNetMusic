package com.mengsama.mod.mengsamanetmusic.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliOpener {
    private static final ResourceLocation BOOK = new ResourceLocation("mengsamanetmusic", "mengsamanetmusic_manual");

    public static void open(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK);
    }
}
