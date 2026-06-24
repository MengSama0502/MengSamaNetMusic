package com.mengsama.mod.mengsamanetmusic.item;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.fabricmc.api.Dist;
import net.fabricmc.api.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicListItem extends MusicCDItem {
    private static final String LIST_KEY = "NetMusicSongInfoList";

    public MusicListItem() {
        super();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    public static List<SongInfo> getSongInfoList(ItemStack stack) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(LIST_KEY)) {
                List<SongInfo> list = new ArrayList<>();
                for (Tag compound : tag.getList(LIST_KEY, Tag.TAG_COMPOUND)) {
                    CompoundTag c = (CompoundTag) compound;
                    list.add(SongInfo.deserializeNBT(c));
                }
                return list;
            }
        }
        return new ArrayList<>();
    }

    public static int getSongCount(ItemStack stack) {
        return getSongInfoList(stack).size();
    }

    public static void nextMusic(ItemStack stack) {
        if (stack.getItem() instanceof MusicListItem) {
            switch (getPlayMode(stack)) {
                case RANDOM -> {
                    int size = getSongInfoList(stack).size();
                    if (size > 1) {
                        int i = RandomSource.create().nextInt(0, size);
                        setSongIndex(stack, i);
                    }
                }
                case SEQUENTIAL -> {
                    int i = getSongIndex(stack) + 1;
                    if (i >= getSongInfoList(stack).size()) {
                        i = 0;
                    }
                    setSongIndex(stack, i);
                }
                case LOOP -> {

                }
            }
        }
    }

    public static SongInfo getSongInfo(ItemStack stack) {
        List<SongInfo> list = getSongInfoList(stack);
        if (list.isEmpty()) {
            return null;
        }
        int i = getSongIndex(stack);
        if (i < 0 || i >= list.size()) {

            i = 0;
            setSongIndex(stack, 0);
        }
        return list.get(i);
    }

    public static int getSongIndex(ItemStack stack) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            if (!tag.contains("index")) {
                tag.putInt("index", 0);
                return 0;
            } else {
                return tag.getInt("index");
            }
        }
        return -1;
    }

    public static void deleteSong(ItemStack stack, int index) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(LIST_KEY)) {
                ListTag list = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
                if (index >= 0 && index < list.size()) {
                    list.remove(index);
                    tag.put(LIST_KEY, list);
                    tag.putInt("index", 0);
                    stack.setTag(tag);
                }
            }
        }
    }

    public static void moveSong(ItemStack stack, int from, int to) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains(LIST_KEY)) {
                ListTag list = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
                if (from >= 0 && from < list.size() && to >= 0 && to < list.size()) {
                    Tag temp = list.get(from);
                    list.set(from, list.get(to));
                    list.set(to, temp);
                    tag.put(LIST_KEY, list);
                    stack.setTag(tag);
                }
            }
        }
    }

    public static void setSongIndex(ItemStack stack, int index) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("index", index);
            stack.setTag(tag);
        }
    }

    public static ItemStack addSongInfo(SongInfo info, ItemStack stack) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            ListTag listTag = tag.contains(LIST_KEY) ? tag.getList(LIST_KEY, Tag.TAG_COMPOUND) : new ListTag();
            CompoundTag sn = new CompoundTag();
            SongInfo.serializeNBT(info, sn);
            listTag.add(sn);
            tag.put(LIST_KEY, listTag);

            if (listTag.size() == 1) {
                tag.putInt("index", 0);
            }
            stack.setTag(tag);
        }
        return stack;
    }

    public static ItemStack setSongInfo(SongInfo info, ItemStack stack) {
        if (stack.getItem() instanceof MusicListItem) {
            CompoundTag tag = stack.getOrCreateTag();
            List<SongInfo> list = getSongInfoList(stack);
            int currentIndex = getSongIndex(stack);

            if (currentIndex >= list.size()) {

                ListTag listTag = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
                CompoundTag sn = new CompoundTag();
                SongInfo.serializeNBT(info, sn);
                listTag.add(sn);
                tag.put(LIST_KEY, listTag);

                setSongIndex(stack, listTag.size());
                stack.setTag(tag);
                return stack;
            }

            SongInfo existing = list.get(currentIndex);
            if (existing == null) {
                return stack;
            }

            ListTag listTag = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
            CompoundTag sn = new CompoundTag();
            SongInfo.serializeNBT(info, sn);
            if (currentIndex < listTag.size()) {
                listTag.set(currentIndex, sn);
            } else {
                listTag.add(sn);
            }
            tag.put(LIST_KEY, listTag);
            stack.setTag(tag);
        }
        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> componentList, @NotNull TooltipFlag flag) {
        String name;
        String text;
        name = Component.translatable("tooltip.mengsamanetmusic.play_mode").getString();
        text = "§a▍ §7" + name + ": §6" + getPlayMode(stack).getName().getString();
        if (getSongInfoList(stack).isEmpty()) {
            componentList.add(Component.translatable("tooltips.mengsamanetmusic.cd.empty").withStyle(ChatFormatting.RED));
        }

        componentList.add(Component.literal(text));
        SongInfo info = getSongInfo(stack);
        if (info != null) {
            if (info.transName != null && !info.transName.isEmpty()) {
                name = Component.translatable("tooltips.mengsamanetmusic.cd.trans_name").getString();
                text = "§a▍ §7" + name + ": §6" + info.transName;
                componentList.add(Component.literal(text));
            }

            if (info.artists != null && !info.artists.isEmpty()) {
                text = StringUtils.join(info.artists, " | ");
                name = Component.translatable("tooltips.mengsamanetmusic.cd.artists").getString();
                text = "§a▍ §7" + name + ": §3" + text;
                componentList.add(Component.literal(text));
            }

            name = Component.translatable("tooltips.mengsamanetmusic.cd.time").getString();
            text = "§a▍ §7" + name + ": §5" + getSongTime(info.songTime);
            componentList.add(Component.literal(text));
        }
    }

    private String getSongTime(int songTime) {
        int min = songTime / 60;
        int sec = songTime % 60;
        String minStr = min <= 9 ? "0" + min : "" + min;
        String secStr = sec <= 9 ? "0" + sec : "" + sec;
        String format = Component.translatable("tooltips.mengsamanetmusic.cd.time.format").getString();
        return String.format(format, minStr, secStr);
    }

    public static PlayMode getPlayMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("play_mode")) {
            setPlayMode(stack, PlayMode.LOOP);
            return PlayMode.LOOP;
        }
        return PlayMode.getMode(tag.getInt("play_mode"));
    }

    public static void setPlayMode(ItemStack stack, PlayMode mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("play_mode", mode.ordinal());
        stack.setTag(tag);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getPlayer().getMainHandItem();
        if (!(stack.getItem() instanceof MusicListItem)) {
            return InteractionResult.PASS;
        }

        var blockState = context.getLevel().getBlockState(context.getClickedPos());
        boolean isMusicPlayer = blockState.getBlock() instanceof com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock;

        if (isMusicPlayer) {

            List<SongInfo> songs = getSongInfoList(stack);
            if (!songs.isEmpty() && getSongIndex(stack) >= songs.size()) {
                setSongIndex(stack, 0);
            }
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            openSelectionScreen(stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MusicListItem)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            openSelectionScreen(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    private void openSelectionScreen(ItemStack stack) {
        List<SongInfo> songs = getSongInfoList(stack);
        int currentIdx = getSongIndex(stack);
        if (songs.isEmpty()) {

            currentIdx = 0;
        } else if (currentIdx < 0 || currentIdx >= songs.size()) {
            currentIdx = 0;
        }
        com.mengsama.mod.mengsamanetmusic.gui.MusicSelectionScreen.open(songs, getPlayMode(stack), currentIdx);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        List<SongInfo> songs = getSongInfoList(stack);
        if (songs.isEmpty()) {

            return Component.translatable("item.mengsamanetmusic.music_list.name", 0);
        }

        SongInfo info = getSongInfo(stack);
        if (info != null && info.songName != null && !info.songName.isEmpty()) {
            return Component.translatable("item.mengsamanetmusic.music_list.info", info.songName);
        }
        return Component.translatable("item.mengsamanetmusic.music_list.name", songs.size());
    }
}
