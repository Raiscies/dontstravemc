package com.dontstravemc.render.renderer;

import com.dontstravemc.DontstravemcClient;
import com.dontstravemc.entity.animal.ButterflyEntity;
import com.dontstravemc.model.ButterflyModel;
import com.dontstravemc.render.state.ButterflyRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ButterflyRenderer extends MobRenderer<ButterflyEntity, ButterflyRenderState, ButterflyModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/butterfly.png");

    public ButterflyRenderer(EntityRendererProvider.Context context) {
        // 核心：使用 context.bakeLayer 获取模型部位并传给模型构造函数
        super(context, new ButterflyModel(context.bakeLayer(DontstravemcClient.BUTTERFLY_LAYER)), 0.2F);
    }

    // 在 ButterflyRenderer 类中

    @Override
    public ResourceLocation getTextureLocation(ButterflyRenderState state) {
        return TEXTURE;
    }

    @Override
    public ButterflyRenderState createRenderState() {
        return new ButterflyRenderState();
    }

    @Override
    public void extractRenderState(ButterflyEntity entity, ButterflyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isResting = entity.isResting();
        state.isDead = !entity.isAlive();
        // 同步三个状态机
        state.flyAnimationState.copyFrom(entity.flyAnimationState);
        state.stayAnimationState.copyFrom(entity.stayAnimationState);
        state.dieAnimationState.copyFrom(entity.dieAnimationState);
    }

    @Override
    protected void setupRotations(
            ButterflyRenderState state,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw
    ) {
        if (state.isDead) {
            return;
        }
        super.setupRotations(state, poseStack, ageInTicks, rotationYaw);
    }
}

