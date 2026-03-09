package com.dontstravemc.model.block;

import com.dontstrave.dontstrave;
import com.dontstravemc.block.entity.custom.TwigBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TwigBlockModel extends GeoModel<TwigBlockEntity> {
    @Override
    public ResourceLocation getModelResource(TwigBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "geo/block/twig.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TwigBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "textures/block/twig.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TwigBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "animations/block/twig.animation.json");
    }
}
