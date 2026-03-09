package com.dontstravemc.loot;

import com.dontstravemc.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModLootTableModifiers {

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {

            // 1. 检查僵尸 (原版)
            if (EntityType.ZOMBIE.getDefaultLootTable().map(ResourceKey::location).orElse(null) == key.location()) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModItems.MONSTER_MEAT))
                        .when(LootItemRandomChanceCondition.randomChance(0.5f))
                );
            }

            // 2. 检查蜘蛛 (原版)
            if (EntityType.SPIDER.getDefaultLootTable().map(ResourceKey::location).orElse(null) == key.location()) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(ModItems.MONSTER_MEAT))
                        .when(LootItemRandomChanceCondition.randomChance(0.35f))
                );
            }



        });
    }
}
