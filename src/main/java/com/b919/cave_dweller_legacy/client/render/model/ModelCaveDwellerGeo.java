package com.b919.cave_dweller_legacy.client.render.model;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ModelCaveDwellerGeo extends AnimatedGeoModel<CaveDwellerEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(CDLMain.MODID, "geo/cave_dweller.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(CDLMain.MODID, "textures/entity/cave_dweller.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(CDLMain.MODID, "animations/cave_dweller.animation.json");

    @Override
    public ResourceLocation getModelLocation(CaveDwellerEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(CaveDwellerEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(CaveDwellerEntity entity) {
        return ANIMATION;
    }
}
