package com.dontstravemc.dsfood.custom;

import com.dontstravemc.status.ModComponents;
import com.dontstravemc.damagesource.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DsFoodItem extends Item {
    private final float healthMod;  // 生命变化
    private final float sanityMod;  // 理智变化
    private final boolean isMonsterFood; // 是否是怪物肉（触发伤害色偏等）

    public DsFoodItem(Properties properties, float healthMod, float sanityMod, boolean isMonsterFood) {
        super(properties);
        this.healthMod = healthMod;
        this.sanityMod = sanityMod;
        this.isMonsterFood = isMonsterFood;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide()) {
            // 1. 处理生命值
            if (healthMod > 0) {
                entity.heal(healthMod);
            } else if (healthMod < 0) {
                // 如果是怪物肉造成的伤害，使用特殊伤害类型
                entity.hurt(isMonsterFood ?
                        level.damageSources().source(ModDamageTypes.EATEN_MONSTER_FOOD) :
                        level.damageSources().generic(), -healthMod);
            }

            // 2. 处理理智值
            if (entity instanceof ServerPlayer player) {
                ModComponents.SANITY.get(player).addSanity(sanityMod);
            }
        }
        return result;
    }
}
