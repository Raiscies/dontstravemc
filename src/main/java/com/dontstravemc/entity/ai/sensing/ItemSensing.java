package com.dontstravemc.entity.ai.sensing;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ItemSensing {
    /**
     * 寻找半径内最近的、符合条件的物品
     */
    public static ItemEntity findNearestFood(Mob mob, double range, Predicate<ItemStack> foodPredicate) {
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, mob.getBoundingBox().inflate(range),
                item -> foodPredicate.test(item.getItem()));

        return items.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}
