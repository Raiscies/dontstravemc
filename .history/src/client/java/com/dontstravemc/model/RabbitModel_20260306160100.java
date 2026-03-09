package com.dontstravemc.model;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.render.state.RabbitRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RabbitModel extends GeoModel<RabbitEntity> {
    @Override
    public ResourceLocation getModelResource(RabbitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/models/entity/ds_rabbit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RabbitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/rabbit/ds_rabbit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RabbitEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/animations/entity/ds_rabbit.animation.json");
    }
}
