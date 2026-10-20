package com.b919.cave_dweller_legacy.client.render;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class RenderCaveDweller extends RenderLiving<CaveDwellerEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CDLMain.MODID, "textures/entity/cave_dweller.png");
    private static final ResourceLocation TEXTURE_GLOW = new ResourceLocation(CDLMain.MODID, "textures/entity/cave_dweller_eyes.png");

    public RenderCaveDweller(RenderManager manager) {
        super(manager, new ModelZombie(), 0.5F);
        this.addLayer(new LayerCaveDwellerEyes(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(@Nonnull CaveDwellerEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void preRenderCallback(CaveDwellerEntity entitylivingbaseIn, float partialTickTime) {
        if (entitylivingbaseIn.isInvisible()) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.0F);
        }
    }
}
