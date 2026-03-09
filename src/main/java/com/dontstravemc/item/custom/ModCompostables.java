package com.dontstravemc.item.custom;

import com.dontstravemc.item.ModItems;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;

public class ModCompostables {

    /**
     * 注册所有模组中可以被堆肥的物品。
     * 在主类的 onInitialize 方法中调用此方法。
     */
    public static void registerCompostables() {
        // 将腐烂食物添加到堆肥桶，有 65% 的几率增加一层
        CompostingChanceRegistry.INSTANCE.add(ModItems.SPOILED_FOOD, 0.65F);

        // 如果未来有其他物品需要堆肥，也在这里添加
        // 例如: CompostingChanceRegistry.INSTANCE.add(ModItems.NITRE, 0.30F);
    }
}
