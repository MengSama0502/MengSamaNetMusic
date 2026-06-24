package com.mengsama.mod.mengsamanetmusic.util;

import net.minecraft.network.chat.Component;

public enum PlayMode {
    LOOP, SEQUENTIAL, RANDOM;

    public Component getName() {
        return Component.translatable("button.mengsamanetmusic." + this.name().toLowerCase());
    }

    public PlayMode getNext() {
        switch (this) {
            case LOOP -> {
                return SEQUENTIAL;
            }
            case SEQUENTIAL -> {
                return RANDOM;
            }
            case RANDOM -> {
                return LOOP;
            }
        }
        return LOOP;
    }

    public static PlayMode getMode(Integer i) {
        if (i == null) return LOOP;
        switch (i) {
            case 0 -> {
                return LOOP;
            }
            case 1 -> {
                return SEQUENTIAL;
            }
            case 2 -> {
                return RANDOM;
            }
        }
        return LOOP;
    }
}
