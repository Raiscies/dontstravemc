package com.dontstravemc.dsfood.custom;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HealingFoodItem extends Item {
    private final float healAmount;

    public HealingFoodItem(Properties properties, float healAmount) {
        super(properties);
        this.healAmount = healAmount;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 先执行原有的吃掉逻辑（减少堆叠数量、增加饱食度）
        ItemStack result = super.finishUsingItem(stack, level, entity);

        // 如果是在服务端，且实体活着，执行回血
        if (!level.isClientSide() && entity.isAlive()) {
            entity.heal(healAmount); // heal 方法直接增加生命值
        }

        return result;
    }
}
