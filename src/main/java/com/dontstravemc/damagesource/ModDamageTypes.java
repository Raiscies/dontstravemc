package com.dontstravemc.damagesource;


import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageScaling;

public interface ModDamageTypes {
    // 1. 定义 ResourceKey (标签)
    ResourceKey<DamageType> EATEN_MONSTER_FOOD = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "eaten_monster_food")
    );

    // 2. 定义 Bootstrap (数据内容)
    static void bootstrap(BootstrapContext<DamageType> context) {
        // 参考你提供的原版代码，这里注册具体属性
        context.register(EATEN_MONSTER_FOOD, new DamageType(
                "eaten_monster_food",
                DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                0F
        ));
    }
}
