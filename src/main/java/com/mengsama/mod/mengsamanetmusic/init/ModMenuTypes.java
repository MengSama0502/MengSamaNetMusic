package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerMenu;
import com.mengsama.mod.mengsamanetmusic.gui.MusicPlayerPlaylistMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.net.fabricmc.fabric;
import net.fabricmc.fabric.DeferredRegister;
import net.fabricmc.fabric.net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.MENU_TYPES, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<MenuType<MusicPlayerMenu>> MUSIC_PLAYER = MENU_TYPES.register("music_player",
            () -> MusicPlayerMenu.TYPE);

    public static final RegistryObject<MenuType<MusicPlayerPlaylistMenu>> MUSIC_PLAYER_PLAYLIST = MENU_TYPES.register("music_player_playlist",
            () -> MusicPlayerPlaylistMenu.TYPE);
}
