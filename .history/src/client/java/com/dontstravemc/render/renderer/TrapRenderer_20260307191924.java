package com.dontstravemc.render.renderer;

import com.dontstravemc.entity.trap.TrapEntity;
import com.dontstravemc.model.TrapModel;
import com.dontstravemc.render.layer.TrapBaitLayer;
import com.dontstravemc.render.state.TrapRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TrapRenderer extends GeoEntityRenderer<TrapEntity, TrapRenderState> {
    
    public TrapRenderer(EntityRendererProvider.Context context) {
        super(context, new TrapModel());
        this.shadowRadius = 0.4F;
        
        // 添加诱饵渲染层
        this.addRenderLayer(new TrapBaitLayer(this));
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
}
