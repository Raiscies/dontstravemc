package com.dontstravemc.model;

import com.dontstravemc.entity.monster.SpiderDenEntity;
import com.dontstravemc.render.state.SpiderDenRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class SpiderDenModel extends GeoModel<SpiderDenEntity> {

    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        // 指向你的 spider_den.geo.json
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "entity/spider_den.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        // 1. 判断 renderState 是否是我们的 SpiderDenRenderState
        // 2. 检查 isDead 字段
        if (renderState instanceof SpiderDenRenderState state) {
            if (state.isDead) {
                // 返回死亡纹理
                return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/spider_den/spider_den_dead.png");
            }
        }

        // 默认纹理
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/spider_den/spider_den.png");
    }


    @Override
    public ResourceLocation getAnimationResource(SpiderDenEntity animatable) {
        // 即使没有动画文件，GeckoLib 有时也需要一个路径以防报错，或者留空
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "animations/entity/spider_den.animation.json");
    }
}