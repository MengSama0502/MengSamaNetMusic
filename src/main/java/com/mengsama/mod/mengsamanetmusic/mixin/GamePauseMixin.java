package com.mengsama.mod.mengsamanetmusic.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class GamePauseMixin {
    @Inject(method = "pauseGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;pause()V"), cancellable = true)
    public void pauseSound(boolean p_91359_, CallbackInfo ci) {

    }
}
