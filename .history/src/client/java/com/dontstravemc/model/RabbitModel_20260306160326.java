package com.dontstravemc.model;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.render.state.RabbitRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class RabbitModel extends GeoModel<RabbitEntity> {
    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/models/entity/ds_rabbit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        // If we want to support sleep texture later, we can check renderState here
        if (renderState instanceof RabbitRenderState s) {
            // For now just return the normal one, but the structure is ready
        }
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/rabbit/ds_rabbit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RabbitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/animations/entity/ds_rabbit.animation.json");
    }
}
