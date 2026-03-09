package com.dontstravemc.render.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * 专门存放蝴蝶渲染所需的数据快照
 */
public class ButterflyRenderState extends LivingEntityRenderState {
    // 必须为 public，以便 Renderer 写入和 Model 读取
    public final AnimationState flyAnimationState = new AnimationState();
    public final AnimationState stayAnimationState = new AnimationState();
    public final AnimationState dieAnimationState = new AnimationState();
    public boolean isResting;
    public boolean isDead;
    public ButterflyRenderState() {
        super();
    }
}