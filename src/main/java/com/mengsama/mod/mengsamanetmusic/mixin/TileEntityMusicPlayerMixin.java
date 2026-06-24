package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.api.QqDiscNbt;
import com.mengsama.mod.mengsamanetmusic.api.QqMusicUpdater;
import com.mengsama.mod.mengsamanetmusic.api.SongInfo;
import com.mengsama.mod.mengsamanetmusic.api.VipCookieState;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.item.MusicCDItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MusicPlayerBlockEntity.class, remap = false)
public abstract class TileEntityMusicPlayerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/mengsama/mod/mengsamanetmusic/item/MusicCDItem;getSongInfo(Lnet/minecraft/world/item/ItemStack;)Lcom/mengsama/mod/mengsamanetmusic/api/SongInfo;"))
    private static SongInfo mengsamanetmusic$refreshAndGetSong(ItemStack stack) {
        SongInfo info = MusicCDItem.getSongInfo(stack);
        if (info != null) {

            if (QqDiscNbt.isQqDisc(stack)) {
                String qqInput = QqDiscNbt.getQqInput(stack);
                int quality = QqDiscNbt.getQuality(stack);
                if (qqInput != null && !qqInput.isBlank()) {
                    String vipCookie = VipCookieState.getEffectiveVipCookie();
                    SongInfo refreshed = QqMusicUpdater.refreshIfNeeded(qqInput, quality, vipCookie);
                    if (refreshed != null && refreshed.songUrl != null && !refreshed.songUrl.isBlank()) {
                        info.songUrl = refreshed.songUrl;
                        MengSamaNetMusic.LOGGER.debug("TileEntityMusicPlayerMixin: refreshed QQ URL for {}", qqInput);
                    }
                }
            }
        }
        return info;
    }
}
