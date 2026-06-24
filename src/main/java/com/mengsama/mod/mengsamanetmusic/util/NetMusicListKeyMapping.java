package com.mengsama.mod.mengsamanetmusic.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.event.RegisterKeyMappingsEvent;
import net.fabricmc.fabric.settings.KeyConflictContext;

public class NetMusicListKeyMapping {
    public static KeyMapping TOGGLE_MUSIC_SPEED_UP;
    public static KeyMapping TOGGLE_MUSIC_TRANSFORM;
    public static KeyMapping FAST_STOP;

    public static void init() {
        TOGGLE_MUSIC_TRANSFORM = new KeyMapping(
                "key.mengsamanetmusic.toggle_music_transform",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LALT,
                "modmenu.nameTranslation.mengsamanetmusic"
        );
        TOGGLE_MUSIC_SPEED_UP = new KeyMapping(
                "key.mengsamanetmusic.toggle_music_speed_up",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_LSHIFT,
                "modmenu.nameTranslation.mengsamanetmusic"
        );
        FAST_STOP = new KeyMapping(
                "key.mengsamanetmusic.toggle_music_fast_stop",
                KeyConflictContext.UNIVERSAL,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                "modmenu.nameTranslation.mengsamanetmusic"
        );
    }

    public static void registerKeyBindings(final RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_MUSIC_SPEED_UP);
        event.register(TOGGLE_MUSIC_TRANSFORM);
        event.register(FAST_STOP);
    }
}
