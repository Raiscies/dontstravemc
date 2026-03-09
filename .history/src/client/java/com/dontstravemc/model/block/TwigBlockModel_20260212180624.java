import com.dontstravemc.render.state.TwigRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class TwigBlockModel extends GeoModel<TwigBlockEntity> {
    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "geo/block/twig.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "textures/block/twig.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TwigBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "animations/block/twig.animation.json");
    }
}
