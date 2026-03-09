package com.dontstravemc.dsfood.custom;

import com.dontstravemc.damagesource.ModDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MonsterFoodItem extends Item {

    private final float damageAmount;
    private final float chance;

    public MonsterFoodItem(Properties properties, float damageAmount, float chance) {
        super(properties);
        this.damageAmount = damageAmount;
        this.chance = chance;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide()) {
            if (level.random.nextFloat() < chance) {
                entity.hurt(level.damageSources().source(ModDamageTypes.EATEN_MONSTER_FOOD), this.damageAmount);
            }
        }
        return result;
    }

}


