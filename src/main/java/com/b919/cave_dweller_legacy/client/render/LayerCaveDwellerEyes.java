package com.b919.cave_dweller_legacy.client.render;

import com.b919.cave_dweller_legacy.CDLMain;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

public class LayerCaveDwellerEyes implements LayerRenderer<CaveDwellerEntity> {
    private static final ResourceLocation EYES = new ResourceLocation(CDLMain.MODID, "textures/entity/cave_dweller_eyes.png");
    private final RenderCaveDweller renderer;

    public LayerCaveDwellerEyes(RenderCaveDweller renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(CaveDwellerEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (entitylivingbaseIn.isInvisible()) {
            return;
        }
        this.renderer.bindTexture(EYES);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(true);
        int i = 61680;
        int j = i % 65536;
        int k = i / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderer.getMainModel().render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        int i1 = entitylivingbaseIn.getBrightnessForRender();
        j = i1 % 65536;
        k = i1 / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
