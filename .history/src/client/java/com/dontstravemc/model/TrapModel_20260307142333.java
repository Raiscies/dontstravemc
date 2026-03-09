package com.dontstravemc.model;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.render.state.TrapRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TrapModel extends GeoModel<TrapEntity, TrapRenderState> {
    
    @Override
    public ResourceLocation getModelResource(TrapEntity animatable, TrapRenderState state) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/models/entity/trap.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TrapEntity animatable, TrapRenderState state) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/trap.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TrapEntity animatable, TrapRenderState state) {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "geckolib/animations/entity/trap.animation.json");
    }

    @Override
    public RenderType getRenderType(TrapEntity animatable, ResourceLocation texture, TrapRenderState state) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
