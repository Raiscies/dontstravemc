package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.model.RabbitModel;
import com.dontstravemc.render.state.RabbitRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
    public int getPackedOverlay(RabbitEntity animatable, Void relatedObject, float u, float partialTick) {
        return net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
    }
}
