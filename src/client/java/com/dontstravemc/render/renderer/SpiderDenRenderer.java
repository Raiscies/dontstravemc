package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.monster.SpiderDenEntity;
import com.dontstravemc.model.SpiderDenModel;
import com.dontstravemc.render.state.SpiderDenRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class SpiderDenRenderer extends GeoEntityRenderer<SpiderDenEntity, SpiderDenRenderState> {

    public SpiderDenRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpiderDenModel());
    }

    // 【修改处】不要重写无参方法，而是重写带参数的方法
    @Override
    public SpiderDenRenderState createRenderState(SpiderDenEntity entity, Void relatedObject) {
        return new SpiderDenRenderState();
    }

    // 这一步依然需要：将实体数据同步给 RenderState
    @Override
    public void extractRenderState(SpiderDenEntity entity, SpiderDenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // 1.21 渲染架构要求：在这里把实体的坐标传给 state
        // 否则渲染时骨骼位置计算会出错
        state.x = entity.getX();
        state.y = entity.getY();
        state.z = entity.getZ();

        state.isDead = entity.isDeadOrDying();
        // 如果有自定义数据（比如等级），也在这里同步
        // state.tier = entity.getTier();
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return 0f;
    }

    @Override
    public int getPackedOverlay(SpiderDenEntity animatable, Void relatedObject, float u, float partialTick) {
        if (animatable.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(animatable, relatedObject, u, partialTick);
    }
}