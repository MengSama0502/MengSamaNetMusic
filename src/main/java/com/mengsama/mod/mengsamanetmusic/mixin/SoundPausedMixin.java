package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.client.audio.ClientMusicPlayback;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
@Implements(@Interface(iface = com.mengsama.mod.mengsamanetmusic.client.PauseSoundManager.class, prefix = "pause$"))
public abstract class SoundPausedMixin {
    @Unique
    private boolean mengsamanetmusic$isPause = false;

    @Inject(method = "pause", at = @At("HEAD"))
    private void onPause(CallbackInfo ci) {
        mengsamanetmusic$isPause = true;
    }

    @Inject(method = "resume", at = @At("TAIL"))
    private void onResume(CallbackInfo ci) {
        mengsamanetmusic$isPause = false;
        ClientMusicPlayback.reapplyDevicePauses();
    }

    @Unique
    public boolean pause$isPaused() {
        return mengsamanetmusic$isPause;
    }
}
