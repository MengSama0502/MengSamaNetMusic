package com.mengsama.mod.mengsamanetmusic.block;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.util.PlayMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.ItemStackHandler;

public interface IMusicPlayerBlockEntity {
    ItemStackHandler getPlayerInv();
    int getPlayIndex();
    void setPlayIndex(int index);
    PlayMode getPlayMode();
    void setPlayMode(PlayMode mode);
    boolean isPlay();
    void setPlay(boolean play);
    ItemStack getCurrentCd();
    void advanceToNext();
    void markDirty();
    void setPlayToClient(SongInfo info);
    Level getLevel();
    net.minecraft.core.BlockPos getBlockPos();
}
