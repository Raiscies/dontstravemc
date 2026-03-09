package com.dontstravemc.render.renderer;

import com.dontstravemc.block.entity.custom.TwigBlockEntity;
import com.dontstravemc.model.block.TwigBlockModel;
import com.dontstravemc.render.state.TwigRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TwigBlockRenderer extends GeoBlockRenderer<TwigBlockEntity, TwigRenderState> {
    public TwigBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new TwigBlockModel());
    }

    @Override
    public TwigRenderState createRenderState(TwigBlockEntity entity, float partialTick) {
        return new TwigRenderState();
    }
}
