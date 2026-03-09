package com.dontstravemc.item.custom;

import com.dontstravemc.item.ModItems; // 确保导入你自己的物品类
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;

public class ModFuelItem {

    public static void registerFuels() {
        // 注册到燃料构建事件
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            // 在这里添加你所有的燃料
            // builder.add(物品, 燃烧时间tick);

            builder.add(ModItems.SPOILED_FOOD, 400);
            builder.add(ModItems.TWIGS, 500); // 5 items × 100 ticks/item

            // 如果还有其他燃料，可以继续在这里添加
            // builder.add(ModItems.SOME_OTHER_FUEL, 1600);
            // builder.add(ModBlocks.SOME_BURNABLE_BLOCK, 2000);
        });

    }
}
