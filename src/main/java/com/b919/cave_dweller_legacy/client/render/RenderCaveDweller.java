package com.b919.cave_dweller_legacy.client.render;

import com.b919.cave_dweller_legacy.client.render.model.ModelCaveDwellerGeo;
import com.b919.cave_dweller_legacy.entity.CaveDwellerEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class RenderCaveDweller extends GeoEntityRenderer<CaveDwellerEntity> {
    public RenderCaveDweller(RenderManager manager) {
        super(manager, new ModelCaveDwellerGeo());
        this.shadowSize = 0.5F;
    }

    @Override
    public void renderEarly(CaveDwellerEntity entity, float limbSwing, float limbSwingAmount, float partialTicks,
                            float ageInTicks, float netHeadYaw) {
        float verticalScale = entity.isCrawling() ? 0.2F : entity.isCrouching() ? 0.49F : 0.73F;
        GlStateManager.scale(0.72F, verticalScale, 0.72F);
    }

    @Override
    public Color getRenderColor(CaveDwellerEntity entity, float partialTicks) {
        return entity.isInvisible() ? Color.ofRGBA(255, 255, 255, 0) : Color.WHITE;
    }
}
