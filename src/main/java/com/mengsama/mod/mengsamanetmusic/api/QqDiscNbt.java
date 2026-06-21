package com.mengsama.mod.mengsamanetmusic.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class QqDiscNbt {
    public static final String ROOT = "MengSamaNetMusicQQ";
    private static final String PROVIDER = "provider";
    private static final String PROVIDER_QQ = "qq";
    private static final String QQ_INPUT = "qq_input";
    private static final String QUALITY = "quality";

    private QqDiscNbt() {
    }

    public static void markQq(ItemStack stack, String input, int qualityOrdinal) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag tag = root.contains(ROOT, Tag.TAG_COMPOUND) ? root.getCompound(ROOT) : new CompoundTag();
        tag.putString(PROVIDER, PROVIDER_QQ);
        if (input != null && !input.isBlank()) {
            tag.putString(QQ_INPUT, input);
        }
        tag.putInt(QUALITY, qualityOrdinal);
        root.put(ROOT, tag);
    }

    public static void clear(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return;
        }
        root.remove(ROOT);
        if (root.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static boolean isQqDisc(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag tag = root.getCompound(ROOT);
        if (tag.contains(PROVIDER, Tag.TAG_STRING)) {
            return PROVIDER_QQ.equalsIgnoreCase(tag.getString(PROVIDER));
        }
        return tag.contains(QQ_INPUT, Tag.TAG_STRING);
    }

    public static String getQqInput(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return "";
        }
        CompoundTag tag = root.getCompound(ROOT);
        return tag.getString(QQ_INPUT);
    }

    public static int getQuality(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag tag = root.getCompound(ROOT);
        if (!tag.contains(QUALITY, Tag.TAG_INT)) {
            return 0;
        }
        return tag.getInt(QUALITY);
    }
}
