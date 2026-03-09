package com.dontstravemc.render.layer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.render.renderer.TrapRenderer;
import com.dontstravemc.render.state.TrapRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

public class TrapBaitItemLayer extends GeoRenderLayer<TrapEntity, Void, TrapRenderState> {
    
    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    
    public TrapBaitItemLayer(TrapRenderer renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, TrapEntity animatable, Void relatedObject, GeoBone bone, 
                              BakedGeoModel bakedModel, TrapRenderState renderState, MultiBufferSource bufferSource, 
                              float partialTick, int packedLight, int packedOverlay) {
        
        // 只在 bait 骨骼上渲染，且有诱饵且未触发时
        if (!"bait".equals(bone.getName()) && !"bait_display".equals(bone.getName())) {
            return;
        }
        
        if (renderState.baitItem.isEmpty() || renderState.isTriggered) {
            return;
        }
        
        poseStack.pushPose();
        
        // 应用骨骼变换
        RenderUtil.translateToPivotPoint(poseStack, bone);
        
        // 调整物品位置和旋转
        poseStack.translate(0, 0.1, 0); // 稍微抬高
        poseStack.scale(0.4F, 0.4F, 0.4F); // 缩小物品
        poseStack.mulPose(Axis.XP.rotationDegrees(90)); // 让物品平放
        poseStack.mulPose(Axis.ZP.rotationDegrees(45)); // 旋转45度更好看
        
        // 使用新的 ItemRenderer API
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        
        // 更新渲染状态
        itemRenderer.updateForTopItem(itemRenderState, renderState.baitItem, ItemDisplayContext.GROUND, animatable.level());
        
        // 渲染物品
        itemRenderer.render(itemRenderState, ItemDisplayContext.GROUND, false, poseStack, bufferSource, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
}
