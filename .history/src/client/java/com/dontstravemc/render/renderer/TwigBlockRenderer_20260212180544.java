package com.dontstravemc.render.renderer;

import com.dontstravemc.block.entity.custom.TwigBlockEntity;
import com.dontstravemc.model.block.TwigBlockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TwigBlockRenderer extends GeoBlockRenderer<TwigBlockEntity> {
    public TwigBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new TwigBlockModel());
    }
}
