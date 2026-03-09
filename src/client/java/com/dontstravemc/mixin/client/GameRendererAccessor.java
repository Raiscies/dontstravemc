package com.dontstravemc.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    // 对应源码中的 private ResourceLocation postEffectId;
    @Accessor("postEffectId")
    void setPostEffectId(ResourceLocation id);

    @Accessor("postEffectId")
    ResourceLocation getPostEffectId();

    // 对应源码中的 private boolean effectActive;
    @Accessor("effectActive")
    void setEffectActive(boolean active);
}