package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicPlayerBlockEntity.class)
public abstract class PlayModeMixin {
    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private static void nextMusic(Level level, BlockPos blockPos, BlockState blockState, MusicPlayerBlockEntity te, CallbackInfo ci) {
        if (!te.isPlay() && !te.getPlayerInv().getStackInSlot(0).isEmpty() && !blockState.getValue(MusicPlayerBlock.CYCLE_DISABLE)) {
            ItemStack stackInSlot = te.getPlayerInv().getStackInSlot(0);
            if (stackInSlot.isEmpty()) {
                return;
            }
            SongInfo songInfo = null;
            if (songInfo != null) {
                te.setPlay(true);
                te.markDirty();
                te.setPlayToClient(songInfo);
            }
        }
    }
}
