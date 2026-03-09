package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.model.RabbitModel;
import com.dontstravemc.render.state.RabbitRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RabbitRenderer extends GeoEntityRenderer<RabbitEntity, RabbitRenderState> {
    public RabbitRenderer(EntityRendererProvider.Context context) {
        super(context, new RabbitModel());
        this.shadowRadius = 0.4F;
    }

    @Override
    public RabbitRenderState createRenderState(RabbitEntity animatable, Void relatedObject) {
        return new RabbitRenderState();
    }

    @Override
    public void extractRenderState(RabbitEntity entity, RabbitRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override
    public void render(RabbitRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(renderState, poseStack, bufferSource, packedLight);
    }

    @Override
    protected int getPackedOverlay(RabbitRenderState renderState, float whiteIntensity) {
        // Force the overlay to a neutral value to disable the red damage tint
        return OverlayTexture.NO_OVERLAY;
    }
}
