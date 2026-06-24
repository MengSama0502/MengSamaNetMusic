package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import com.mengsama.mod.mengsamanetmusic.block.PortableMusicPlayerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPES, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<BlockEntityType<MusicPlayerBlockEntity>> MUSIC_PLAYER = BLOCK_ENTITIES.register("music_player",
            () -> BlockEntityType.Builder.of(MusicPlayerBlockEntity::new, ModBlocks.MUSIC_PLAYER.get()).build(null));

    public static final RegistryObject<BlockEntityType<PortableMusicPlayerBlockEntity>> PORTABLE_MUSIC_PLAYER = BLOCK_ENTITIES.register("portable_music_player",
            () -> BlockEntityType.Builder.of(PortableMusicPlayerBlockEntity::new, ModBlocks.PORTABLE_MUSIC_PLAYER.get()).build(null));

    }
