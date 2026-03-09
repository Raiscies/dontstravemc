package com.dontstravemc.render.layer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.render.renderer.TrapRenderer;
import com.dontstravemc.render.state.TrapRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TrapBaitLayer extends GeoRenderLayer<TrapEntity, Void, TrapRenderState> {
    
    public TrapBaitLayer(TrapRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TrapEntity animatable, Void relatedObject, BakedGeoModel bakedModel, 
                       TrapRenderState renderState, MultiBufferSource bufferSource, float partialTick, 
                       int packedLight, int packedOverlay) {
        
        // 只在有诱饵且未触发时渲染
        if (renderState.baitItem.isEmpty() || renderState.isTriggered) {
            return;
        }
        
        poseStack.pushPose();
        
        // 将物品定位到陷阱中心上方
        poseStack.translate(0.0, 0.15, 0.0);
        
        // 缩小物品尺寸
        poseStack.scale(0.5F, 0.5F, 0.5F);
        
        // 让物品平放
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        
        // 渲染物品
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.render(
            renderState.baitItem,
            ItemDisplayContext.GROUND,
            false,
            poseStack,
            bufferSource,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            itemRenderer.getModel(renderState.baitItem, animatable.level(), null, 0)
        );
        
        poseStack.popPose();
    }
}
