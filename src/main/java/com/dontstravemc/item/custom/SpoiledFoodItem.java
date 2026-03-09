package com.dontstravemc.item.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 腐烂食物：
 * - 食用后减少 1 点饱食度（Food Level）
 * - 其余能力（堆肥/燃料）在初始化阶段通过 Fabric 注册表配置
 */
public class SpoiledFoodItem extends Item {
    public SpoiledFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide() && entity instanceof Player player) {
            // 1.21.x：FoodData#eat(int, float) 支持负数来减少饱食度
            // 避免变成负数
            if (player.getFoodData().getFoodLevel() > 0) {
                player.getFoodData().eat(-1, 0.0F);
            }
        }

        return result;
    }
}

