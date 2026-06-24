package com.mengsama.mod.mengsamanetmusic.network;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MengSamaNetMusic.MOD_ID, "network"),
            () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));

    public static void init() {
        int id = 0;

        CHANNEL.registerMessage(id++, PlayMusicPacket.class,
                PlayMusicPacket::encode, PlayMusicPacket::decode, PlayMusicPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, StopMusicPacket.class,
                StopMusicPacket::encode, StopMusicPacket::decode, StopMusicPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PlayerPlayMusicPacket.class,
                PlayerPlayMusicPacket::encode, PlayerPlayMusicPacket::decode, PlayerPlayMusicPacket::handle,
                Optional.empty());
        CHANNEL.registerMessage(id++, MusicListDataPacket.class,
                MusicListDataPacket::encode, MusicListDataPacket::decode, MusicListDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, DeleteMusicDataPacket.class,
                DeleteMusicDataPacket::encode, DeleteMusicDataPacket::decode, DeleteMusicDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, MoveMusicDataPacket.class,
                MoveMusicDataPacket::encode, MoveMusicDataPacket::decode, MoveMusicDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, UpdateMusicTickPacket.class,
                UpdateMusicTickPacket::encode, UpdateMusicTickPacket::decode, UpdateMusicTickPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, StopMusicPacketServer.class,
                StopMusicPacketServer::encode, StopMusicPacketServer::decode, StopMusicPacketServer::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PlayerAddSongPacket.class,
                PlayerAddSongPacket::encode, PlayerAddSongPacket::decode, PlayerAddSongPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, BlockAddSongPacket.class,
                BlockAddSongPacket::encode, BlockAddSongPacket::decode, BlockAddSongPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PlayerRemoveSongPacket.class,
                PlayerRemoveSongPacket::encode, PlayerRemoveSongPacket::decode, PlayerRemoveSongPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, StopMusicPacketClient.class,
                StopMusicPacketClient::encode, StopMusicPacketClient::decode, StopMusicPacketClient::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SyncVipCookiePacket.class,
                SyncVipCookiePacket::encode, SyncVipCookiePacket::decode, SyncVipCookiePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToNearby(Level world, BlockPos pos, Object toSend) {
        if (world instanceof ServerLevel ws) {
            ws.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 96 * 96)
                    .forEach(p -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), toSend));
        }
    }

    public static void sendToClientPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
