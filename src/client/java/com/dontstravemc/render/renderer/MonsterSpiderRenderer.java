package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import com.dontstravemc.entity.monster.SpiderDenEntity;
import com.dontstravemc.model.MonsterSpiderModel;
import com.dontstravemc.render.state.MonsterSpiderRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class MonsterSpiderRenderer extends GeoEntityRenderer<MonsterSpiderEntity, MonsterSpiderRenderState> {

    public MonsterSpiderRenderer(EntityRendererProvider.Context context) {
        super(context, new MonsterSpiderModel());
        this.shadowRadius = 0.8F;
    }

    // --- 关键修复：重写双参数构造 State 方法 ---
    @Override
    public MonsterSpiderRenderState createRenderState(MonsterSpiderEntity animatable, Void relatedObject) {
        return new MonsterSpiderRenderState();
    }

    @Override
    public void extractRenderState(MonsterSpiderEntity entity, MonsterSpiderRenderState state, float partialTick) {
        // 此时 state 已经是 MonsterSpiderRenderState 实例了，调用 super 是安全的
        super.extractRenderState(entity, state, partialTick);

        // 你的自定义同步逻辑
        state.isAsleep = entity.isAsleep();
        state.isAlert = entity.isAlert();
        state.isAggressive = !entity.isEating() && entity.swinging;
        state.isEating = entity.isEating();
        state.isHurt = entity.hurtTime > 0 || entity.deathTime > 0;
        state.isDead = entity.deathTime > 0 || !entity.isAlive();
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return 0f;
    }

    @Override
    public int getPackedOverlay(MonsterSpiderEntity animatable, Void relatedObject, float u, float partialTick) {
        if (animatable.isDeadOrDying()) {
            return OverlayTexture.NO_OVERLAY;
        }
        return super.getPackedOverlay(animatable, relatedObject, u, partialTick);
    }
}