package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MusicCDItem.class, remap = false)
public abstract class CanPlayListMixin {

    @Inject(method = "getSongInfo", at = @At("HEAD"), cancellable = true)
    private static void mengsamanetmusic$getPlayListInfo(ItemStack stack, CallbackInfoReturnable<SongInfo> cir) {
        if (stack.getItem() instanceof MusicListItem) {
            cir.setReturnValue(MusicListItem.getSongInfo(stack));
        }
    }

    @Inject(method = "setSongInfo", at = @At("HEAD"), cancellable = true)
    private static void mengsamanetmusic$setPlayListInfo(SongInfo info, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() instanceof MusicListItem) {
            cir.setReturnValue(MusicListItem.setSongInfo(info, stack));
        }
    }
}
