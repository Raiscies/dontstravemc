package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.model.TrapModel;
import com.dontstravemc.render.state.TrapRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
        state.baitItem = entity.getBaitItem();
    }

    @Override
    public void render(TrapRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(state, poseStack, bufferSource, packedLight);
        
        // Render bait item if present
        if (!state.baitItem.isEmpty()) {
            poseStack.pushPose();
            
            // Position the bait in the center of the trap, slightly above ground
            poseStack.translate(0, 0.2, 0);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            
            // Render the item
            Minecraft.getInstance().getItemRenderer().renderStatic(
                state.baitItem,
                ItemDisplayContext.GROUND,
                packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                null,
                0
            );
            
            poseStack.popPose();
        }
    }
}
