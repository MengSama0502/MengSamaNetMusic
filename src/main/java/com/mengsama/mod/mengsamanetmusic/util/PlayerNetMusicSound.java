package com.mengsama.mod.mengsamanetmusic.util;

import com.mengsama.mod.mengsamanetmusic.client.audio.NetMusicAudioStream;
import com.mengsama.mod.mengsamanetmusic.init.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class PlayerNetMusicSound extends AbstractTickableSoundInstance {

    public static int currentTick = -1;

    final Player player;
    final URL url;
    final int countTick;
    int tick = 0;
    final int slot;

    @Nullable
    String clientUrl;

    public PlayerNetMusicSound(Player player, URL songUrl, int second, int slot) {
        super(ModSounds.NET_MUSIC.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.url = songUrl;
        this.countTick = second * 20;
        this.volume = 4f;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.slot = slot;

        relative = false;
        attenuation = isClientPlayer() ? Attenuation.NONE : Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (player.isRemoved()) {
            stopMusic();
            return;
        }

        if (isClientPlayer()) {
            var mpItem = com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.findMusicPlayerItem(player);
            if (mpItem.isEmpty() || !com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem.isPlay(mpItem)) {
                stopMusic();
                return;
            }
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            stopMusic();
            return;
        }
        ++this.tick;
        currentTick = this.tick;
        if (this.tick >= this.countTick + 200) {
            stopMusic();
        } else {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            if (level.getGameTime() % 8L == 0L) {
                for (int i = 0; i < 2; ++i) {
                    level.addParticle(ParticleTypes.NOTE, this.x - (double) 0.5F + level.random.nextDouble(), this.y + (double) 1.5F + level.random.nextDouble(), this.z - (double) 0.5F + level.random.nextDouble(), level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextInt(3));
                }
            }
        }
    }

    public void stopMusic() {
        try {
            Minecraft.getInstance().getSoundManager().stop(this);
        } catch (Exception ignored) {}
        this.stop();
        currentTick = -1;
    }

    public void onlyTickUpdate() {
        tick++;
    }

    public int getTick() {
        return tick;
    }

    @Override
    public @NotNull CompletableFuture<AudioStream> getStream(@NotNull SoundBufferLibrary soundBuffers, @NotNull Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new NetMusicAudioStream(url);
            } catch (UnsupportedAudioFileException | IOException e) {

                Minecraft.getInstance().execute(() -> {
                    stopMusic();
                });
                try {
                    byte[] silence = new byte[4];
                    javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(44100, 16, 2, true, false);
                    return new NetMusicAudioStream(new javax.sound.sampled.AudioInputStream(
                            new java.io.ByteArrayInputStream(silence), fmt, 1));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, Util.backgroundExecutor());
    }

    public boolean isClientPlayer() {
        if (Minecraft.getInstance().player != null) {
            return player.getUUID().equals(Minecraft.getInstance().player.getUUID());
        }
        return false;
    }

    public Player getPlayer() {
        return player;
    }
}
