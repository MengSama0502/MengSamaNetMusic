package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.QqDiscNbt;
import com.mengsama.mod.mengsamanetmusic.api.QqMusicUpdater;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.api.VipCookieState;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MusicPlayerBlock.class, remap = false)
public abstract class BlockMusicPlayerMixin {
    @Inject(method = "playerMusic", at = @At("HEAD"), require = 0)
    private static void mengsamanetmusic$refreshBeforeMusic(Level level, BlockPos pos, boolean signal, CallbackInfo ci) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MusicPlayerBlockEntity te) {
            ItemStack stack = te.getPlayerInv().getStackInSlot(0);
            if (!stack.isEmpty()) {
                SongInfo info = null;
                if (info != null && QqDiscNbt.isQqDisc(stack)) {
                    String qqInput = QqDiscNbt.getQqInput(stack);
                    int quality = QqDiscNbt.getQuality(stack);
                    if (qqInput != null && !qqInput.isBlank()) {
                        String vipCookie = VipCookieState.getEffectiveVipCookie();
                        SongInfo refreshed = QqMusicUpdater.refreshIfNeeded(qqInput, quality, vipCookie);
                        if (refreshed != null && refreshed.songUrl != null && !refreshed.songUrl.isBlank()) {
                            info.songUrl = refreshed.songUrl;
                            MengSamaNetMusic.LOGGER.debug("BlockMusicPlayerMixin: refreshed QQ URL for {}", qqInput);
                        }
                    }
                }
            }
        }
    }

    @Redirect(method = "playerMusic", at = @At(value = "INVOKE",
            target = "Lcom/mengsama/mod/mengsamanetmusic/item/MusicCDItem;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/mengsama/mod/mengsamanetmusic/api/SongInfo;"),
            require = 0)
    private static SongInfo mengsamanetmusic$getRefreshedSong(ItemStack stack) {
        SongInfo info = null;
        if (info != null && QqDiscNbt.isQqDisc(stack)) {
            String qqInput = QqDiscNbt.getQqInput(stack);
            int quality = QqDiscNbt.getQuality(stack);
            if (qqInput != null && !qqInput.isBlank()) {
                String vipCookie = VipCookieState.getEffectiveVipCookie();
                SongInfo refreshed = QqMusicUpdater.refreshIfNeeded(qqInput, quality, vipCookie);
                if (refreshed != null && refreshed.songUrl != null && !refreshed.songUrl.isBlank()) {
                    info.songUrl = refreshed.songUrl;
                }
            }
        }
        return info;
    }
}
