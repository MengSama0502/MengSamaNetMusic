package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.QqDiscNbt;
import com.mengsama.mod.mengsamanetmusic.api.QqMusicUpdater;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.api.VipCookieState;
import com.mengsama.mod.mengsamanetmusic.item.MusicListItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.mengsama.mod.mengsamanetmusic.item.MusicListItem", remap = false)
public abstract class NetMusicListItemMixin {
    @Inject(method = "getSongInfo", at = @At("RETURN"), cancellable = true)
    private static void mengsamanetmusic$refreshQqSongInfo(ItemStack stack, CallbackInfoReturnable<SongInfo> cir) {
        SongInfo info = cir.getReturnValue();
        if (info == null) {
            return;
        }

        if (QqDiscNbt.isQqDisc(stack)) {
            String qqInput = QqDiscNbt.getQqInput(stack);
            int quality = QqDiscNbt.getQuality(stack);
            if (qqInput != null && !qqInput.isBlank()) {
                String vipCookie = VipCookieState.getEffectiveVipCookie();
                SongInfo refreshed = QqMusicUpdater.refreshIfNeeded(qqInput, quality, vipCookie);
                if (refreshed != null && refreshed.songUrl != null && !refreshed.songUrl.isBlank()) {
                    info.songUrl = refreshed.songUrl;
                    MengSamaNetMusic.LOGGER.debug("NetMusicListItemMixin: refreshed QQ URL for {}", qqInput);
                }
            }
        }
        cir.setReturnValue(info);
    }
}
