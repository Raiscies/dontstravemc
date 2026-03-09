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
}
