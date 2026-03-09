package com.dontstravemc.model;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.render.state.TrapRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class TrapModel extends GeoModel<TrapEntity> {
    
    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/models/entity/trap.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/trap.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TrapEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/animations/entity/trap.animation.json");
    }
}
