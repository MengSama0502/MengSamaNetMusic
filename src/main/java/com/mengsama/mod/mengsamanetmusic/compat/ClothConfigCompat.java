package com.mengsama.mod.mengsamanetmusic.compat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.ConfigScreenHandler;
import net.fabricmc.fabric.ModLoadingContext;

public class ClothConfigCompat {
    public static void registerModsPage() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
                new ConfigScreenHandler.ConfigScreenFactory((client, parent) ->
                        getConfigBuilder().setParentScreen(parent).build()));
    }

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create().setTitle(Component.translatable("itemGroup.mengsamanetmusic"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = root.entryBuilder();
        ConfigCategory general = root.getOrCreateCategory(Component.translatable("config.mengsamanetmusic.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.mengsamanetmusic.general.enable_cache"), false)
                .setTooltip(Component.translatable("config.mengsamanetmusic.general.enable_cache.tooltip"))
                .setDefaultValue(false)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.mengsamanetmusic.general.enable_vip_bypass"), false)
                .setTooltip(Component.translatable("config.mengsamanetmusic.general.enable_vip_bypass.tooltip"))
                .setDefaultValue(false)
                .build());

        return root;
    }
}
