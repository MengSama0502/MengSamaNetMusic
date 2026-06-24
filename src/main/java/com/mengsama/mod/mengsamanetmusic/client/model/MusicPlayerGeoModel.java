package com.mengsama.mod.mengsamanetmusic.client.model;

import com.mengsama.mod.mengsamanetmusic.MengSamaNetMusic;
import com.mengsama.mod.mengsamanetmusic.block.MusicPlayerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedGeoModel;

public class MusicPlayerGeoModel extends DefaultedGeoModel<MusicPlayerBlockEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(MengSamaNetMusic.MOD_ID, "geo/music_player.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(MengSamaNetMusic.MOD_ID, "textures/block/music_player_block.png");

    public MusicPlayerGeoModel() {
        super(new ResourceLocation(MengSamaNetMusic.MOD_ID, "music_player"));
    }

    @Override
    public ResourceLocation getModelResource(MusicPlayerBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MusicPlayerBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public String subtype() {
        return "";
    }
}
