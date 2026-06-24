package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.cache.MusicCache;
import com.mengsama.mod.mengsamanetmusic.util.NetMusicListUtil;
import com.mengsama.mod.mengsamanetmusic.util.NetWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = NetWorker.class, remap = false)
public abstract class GetMusicMixin {
    @Inject(method = "getRedirectUrl", at = @At("HEAD"), remap = false, cancellable = true)
    private static void onGetRedirectUrlHead(String url, Map<String, String> headers, CallbackInfoReturnable<String> cir) {
        if (url != null && url.contains("music.163.com")) {
            try {
                long id = NetMusicListUtil.getIdFromUrl(url);
                java.nio.file.Path cacheFile = MusicCache.getCacheFilePath(Long.toString(id));
                if (java.nio.file.Files.exists(cacheFile)) {
                    String cachedUrl = cacheFile.toUri().toURL().toString();
                    cir.setReturnValue(cachedUrl);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(method = "getRedirectUrl", at = @At("RETURN"), remap = false, cancellable = true)
    private static void onGetRedirectUrl(String url, Map<String, String> headers, CallbackInfoReturnable<String> cir) {

        if (url != null && url.contains("music.163.com")) {
            try {
                long id = NetMusicListUtil.getIdFromUrl(url);
                java.nio.file.Path cacheFile = MusicCache.getCacheFilePath(Long.toString(id));
                if (java.nio.file.Files.exists(cacheFile)) {
                    String cachedUrl = cacheFile.toUri().toURL().toString();
                    cir.setReturnValue(cachedUrl);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
