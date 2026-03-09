package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.model.TrapModel;
import com.dontstravemc.render.state.TrapRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TrapRenderer extends GeoEntityRenderer<TrapEntity, TrapRenderState> {
    
    private final ItemModelResolver itemModelResolver;
    
    public TrapRenderer(EntityRendererProvider.Context context) {
        super(context, new TrapModel());
        this.shadowRadius = 0.4F;
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public TrapRenderState createRenderState(TrapEntity animatable, Void relatedObject) {
        return new TrapRenderState();
    }

    @Override
    public void extractRenderState(TrapEntity entity, TrapRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isTriggered = entity.isTriggered();
        
        // 使用ItemModelResolver更新物品渲染状态，类似ItemFrame的做法
        this.itemModelResolver.updateForNonLiving(
            state.baitItem, 
            entity.getBaitItem(), 
            ItemDisplayContext.FIXED, 
            entity
        );
    }
    
    @Override
    public void submit(TrapRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
        // 先渲染陷阱本体
        super.submit(renderState, poseStack, submitNodeCollector, cameraState);
        
        // 如果有诱饵且陷阱未触发，渲染诱饵物品
        if (!renderState.isTriggered && !renderState.baitItem.isEmpty()) {
            poseStack.pushPose();
            
            // 尝试获取bait骨骼的位置
            GeoBone baitBone = this.getGeoModel().getBone("bait").orElse(null);
            
            if (baitBone != null) {
                // 使用bait骨骼的位置（转换为方块单位，GeckoLib使用像素单位）
                poseStack.translate(
                    baitBone.getPivotX() / 16.0,
                    baitBone.getPivotY() / 16.0,
                    baitBone.getPivotZ() / 16.0
                );
            } else {
                // 如果找不到bait骨骼，使用默认位置（陷阱中心）
                poseStack.translate(0.0, 0.0, 0.0);
            }
            
            // 让物品平躺在地上：绕X轴旋转90度
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            
            // 缩放0.4倍
            poseStack.scale(0.4F, 0.4F, 0.4F);
            
            // 使用ItemStackRenderState的submit方法渲染物品
            renderState.baitItem.submit(
                poseStack,
                submitNodeCollector,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor
            );
            
            poseStack.popPose();
        }
    }
}
