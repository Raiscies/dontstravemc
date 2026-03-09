package com.dontstravemc.entity.util.animationutil; // 另起炉灶到 util 包

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CombatAnimationHelper {

    /**
     * 辅助处理攻击动画的触发
     * @param attacker 攻击者
     * @param target 目标
     * @param hurtSupplier 原始的伤害逻辑 Lambda
     * @return 伤害是否成功
     */
    public static boolean executeHurtWithSwing(LivingEntity attacker, Entity target, java.util.function.BooleanSupplier hurtSupplier) {
        boolean hurt = hurtSupplier.getAsBoolean();
        if (hurt) {
            // 核心逻辑：触发挥动手臂，从而让 GeckoLib 的 controller 检测到 swinging
            attacker.swing(InteractionHand.MAIN_HAND);
        }
        return hurt;
    }
}