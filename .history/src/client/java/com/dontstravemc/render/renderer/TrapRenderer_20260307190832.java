package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.model.TrapModel;
import com.dontstravemc.render.state.TrapRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TrapRenderer extends GeoEntityRenderer<TrapEntity, TrapRenderState> {
    
    public TrapRenderer(EntityRendererProvider.Context context) {
        super(context, new TrapModel());
        this.shadowRadius = 0.4F;
    }

    @Override
    public TrapRenderState createRenderState(TrapEntity animatable, Void relatedObject) {
        return new TrapRenderState();
    }

    @Override
    public void extractRenderState(TrapEntity entity, TrapRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isTriggered = entity.isTriggered();
        state.baitItem = entity.getBaitItem().copy();
    }

    @Override
    public void renderFinal(TrapRenderState renderState, PoseStack poseStack, BakedGeoModel model, SubmitNodeCollector renderTasks, CameraRenderState cameraState, int packedLight, int packedOverlay, int renderColor) {
        super.renderFinal(renderState, poseStack, model, renderTasks, cameraState, packedLight, packedOverlay, renderColor);
        
        // 渲染诱饵物品
        if (!renderState.baitItem.isEmpty() && !renderState.isTriggered) {
            poseStack.pushPose();
            
            // 将物品定位到陷阱中心上方
            poseStack.translate(0.0, 0.15, 0.0);
            
            // 缩小物品尺寸
            poseStack.scale(0.5F, 0.5F, 0.5F);
            
            // 让物品平放
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            
            // 渲染物品
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            
            // 使用 renderTasks 来提交渲染任务
            renderTasks.accept((bufferSource) -> {
                itemRenderer.renderStatic(
                    renderState.baitItem,
                    ItemDisplayContext.GROUND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    bufferSource,
                    null,
                    0
                );
            });
            
            poseStack.popPose();
        }
    }
}
