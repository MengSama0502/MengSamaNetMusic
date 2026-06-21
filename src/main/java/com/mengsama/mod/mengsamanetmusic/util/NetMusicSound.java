package com.mengsama.mod.mengsamanetmusic.util;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.block.IMusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.client.audio.NetMusicAudioStream;
import com.mengsama.mod.mengsamanetmusic.init.ModSounds;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class NetMusicSound extends AbstractTickableSoundInstance {
    public static final ResourceLocation ERROR_SOUND = new ResourceLocation(MengSamaNetMusic.MOD_ID, "sounds/error.ogg");

    public static int currentTick = -1;

    final BlockPos pos;
    final URL url;
    final int countTick;
    int tick = 0;

    public NetMusicSound(BlockPos pos, URL songUrl, int second) {
        super(ModSounds.NET_MUSIC.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.pos = pos;
        this.url = songUrl;
        this.countTick = second * 20;
        this.volume = 4.0F;
        this.x = (float) pos.getX() + 0.5F;
        this.y = (float) pos.getY() + 0.5F;
        this.z = (float) pos.getZ() + 0.5F;

        relative = false;
        attenuation = Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        tick++;
        currentTick = tick;
        if (tick > countTick + 50) {
            this.stop();
            currentTick = -1;
        } else {
            if (world.getGameTime() % 8 == 0) {
                for (int i = 0; i < 2; i++) {
                    world.addParticle(ParticleTypes.NOTE,
                            x - 0.5f + world.random.nextDouble(),
                            y + world.random.nextDouble() + 1,
                            z - 0.5f + world.random.nextDouble(),
                            world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextInt(3));
                }
            }
        }

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof IMusicPlayerBlockEntity musicPlay) {
            if (!musicPlay.isPlay()) {
                stopSound();
            }
        } else {
            stopSound();
        }
    }

    private void errorStop() {
        this.tick = countTick;
        MutableComponent error = Component.translatable("message.mengsamanetmusic.music_player.play_error");
        Minecraft.getInstance().gui.setOverlayMessage(error, false);
    }

    public void stopSound() {
        try {
            Minecraft.getInstance().getSoundManager().stop(this);
        } catch (Exception ignored) {}
        this.stop();
        currentTick = -1;
    }

    public int getTick() {
        return tick;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public @NotNull CompletableFuture<AudioStream> getStream(@NotNull SoundBufferLibrary soundBuffers, @NotNull Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new NetMusicAudioStream(this.url);
            } catch (IOException | UnsupportedAudioFileException e) {
                MengSamaNetMusic.LOGGER.error("Failed to create audio stream for URL: {}", url, e);
                Minecraft.getInstance().submit(this::errorStop);
            }

            try {
                InputStream inputstream = Minecraft.getInstance().getResourceManager().open(ERROR_SOUND);
                return new OggAudioStream(inputstream);
            } catch (IOException ioexception) {
                throw new CompletionException(ioexception);
            }
        }, Util.backgroundExecutor());
    }
}
