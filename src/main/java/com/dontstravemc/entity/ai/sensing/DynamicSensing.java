package com.dontstravemc.entity.ai.sensing;


import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;


public class DynamicSensing {

    /**
     * 通用的昼夜范围调整逻辑
     * 现在支持根据百分比调整，而不是写死数值，更加通用
     */
    public static void updateFollowRangeByTime(Mob mob, double dayMultiplier, double nightMultiplier) {
        if (mob.level().isClientSide()) return;

        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            // 获取初始的基础属性值（比如蜘蛛默认是16）
            double defaultValue = followRange.getAttribute().value().getDefaultValue();
            double targetValue = mob.level().isBrightOutside() ?
                    defaultValue * dayMultiplier :
                    defaultValue * nightMultiplier;

            if (followRange.getBaseValue() != targetValue) {
                followRange.setBaseValue(targetValue);
            }
        }
    }

    /**
     * 高级玩家探测：自动过滤掉创造模式、旁观模式
     */
    public static boolean isValidPlayerNearby(Mob mob, double range) {
        if (mob.level().isClientSide()) return false;

        // 根据你提供的 EntityGetter 源码:
        // getNearestPlayer(double x, double y, double z, double range, boolean checkCanBeAttacked)
        // 最后一个参数传入 true，它会自动调用 EntitySelector.NO_CREATIVE_OR_SPECTATOR
        Player nearestPlayer = mob.level().getNearestPlayer(
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                range,
                true  // 这个 true 会过滤掉创造/旁观模式
        );

        // 依然需要检查视线
        return nearestPlayer != null && mob.hasLineOfSight(nearestPlayer);
    }
}