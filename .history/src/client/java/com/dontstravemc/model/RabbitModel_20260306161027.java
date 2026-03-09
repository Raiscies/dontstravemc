package com.dontstravemc.model;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.render.state.RabbitRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class RabbitModel extends GeoModel<RabbitEntity> {
    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        // Aligned with MonsterSpider style
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "entity/ds_rabbit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/rabbit/ds_rabbit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RabbitEntity animatable) {
        // Aligned with MonsterSpider style (no extension)
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "entity/ds_rabbit");
    }
}
