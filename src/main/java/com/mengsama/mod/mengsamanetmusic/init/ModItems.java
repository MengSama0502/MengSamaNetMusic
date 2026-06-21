package com.mengsama.mod.mengsamanetmusic.init;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.item.MusicPlayerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MengSamaNetMusic.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MengSamaNetMusic.MOD_ID);

    public static final RegistryObject<Item> MUSIC_PLAYER = ITEMS.register("music_player",
            () -> new MusicPlayerItem(ModBlocks.PORTABLE_MUSIC_PLAYER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MUSIC_PLAYER_BLOCK = ITEMS.register("music_player_block",
            () -> new BlockItem(ModBlocks.MUSIC_PLAYER.get(), new Item.Properties()));
    public static final RegistryObject<Item> MOD_ICON = ITEMS.register("mod_icon",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("tab",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.mengsamanetmusic"))
                    .icon(() -> new ItemStack(MOD_ICON.get()))
                    .displayItems((params, output) -> {
                        output.accept(MUSIC_PLAYER.get());
                        output.accept(MUSIC_PLAYER_BLOCK.get());
                    }).build());
}