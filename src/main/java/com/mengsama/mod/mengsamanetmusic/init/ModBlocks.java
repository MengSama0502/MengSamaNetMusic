package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlock;
import com.mengsama.mod.mengsamanetmusic.block.PortableMusicPlayerBlock;
import net.minecraft.world.level.block.Block;
import net.fabricmc.fabric.DeferredRegister;
import net.fabricmc.fabric.net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCKS, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<Block> MUSIC_PLAYER = BLOCKS.register("music_player",
            () -> new MusicPlayerBlock());

    public static final RegistryObject<Block> PORTABLE_MUSIC_PLAYER = BLOCKS.register("portable_music_player",
            () -> new PortableMusicPlayerBlock());

    }
