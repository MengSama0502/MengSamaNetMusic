package com.mengsama.mod.mengsamanetmusic.item;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class MusicCDItem extends Item {
    public static final String SONG_INFO_TAG = "NetMusicSongInfo";

    public MusicCDItem() {
        super(new Properties());
    }

    public static SongInfo getSongInfo(ItemStack stack) {
        if (stack.getItem() instanceof MusicCDItem) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(SONG_INFO_TAG, Tag.TAG_COMPOUND)) {
                CompoundTag infoTag = tag.getCompound(SONG_INFO_TAG);
                return SongInfo.deserializeNBT(infoTag);
            }
        }
        return null;
    }

    public static ItemStack setSongInfo(SongInfo info, ItemStack stack) {
        if (stack.getItem() instanceof MusicCDItem) {
            CompoundTag tag = stack.getOrCreateTag();
            CompoundTag songInfoTag = new CompoundTag();
            SongInfo.serializeNBT(info, songInfoTag);
            tag.put(SONG_INFO_TAG, songInfoTag);
            stack.setTag(tag);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        SongInfo info = getSongInfo(stack);
        if (info != null) {
            String name = info.songName;
            if (info.vip) {
                name = name + " §4§l[VIP]";
            }
            if (info.readOnly) {
                MutableComponent readOnlyText = Component.translatable("tooltips.mengsamanetmusic.cd.read_only").withStyle(ChatFormatting.YELLOW);
                return Component.literal(name).append(CommonComponents.SPACE).append(readOnlyText);
            }
            return Component.literal(name);
        }
        return super.getName(stack);
    }

    private String getSongTime(int songTime) {
        int min = songTime / 60;
        int sec = songTime % 60;
        String minStr = min <= 9 ? ("0" + min) : ("" + min);
        String secStr = sec <= 9 ? ("0" + sec) : ("" + sec);
        return minStr + ":" + secStr;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        SongInfo info = getSongInfo(stack);
        final String prefix = "§a▍ §7";
        final String delimiter = ": ";
        if (info != null) {
            if (StringUtils.isNoneBlank(info.transName)) {
                String text = prefix + Component.translatable("tooltips.mengsamanetmusic.cd.trans_name").getString() + delimiter + "§6" + info.transName;
                tooltip.add(Component.literal(text));
            }
            if (info.artists != null && !info.artists.isEmpty()) {
                String artistNames = StringUtils.join(info.artists, " | ");
                String text = prefix + Component.translatable("tooltips.mengsamanetmusic.cd.artists").getString() + delimiter + "§3" + artistNames;
                tooltip.add(Component.literal(text));
            }
            String text = prefix + Component.translatable("tooltips.mengsamanetmusic.cd.time").getString() + delimiter + "§5" + getSongTime(info.songTime);
            tooltip.add(Component.literal(text));
        } else {
            tooltip.add(Component.translatable("tooltips.mengsamanetmusic.cd.empty").withStyle(ChatFormatting.RED));
        }
    }
}
