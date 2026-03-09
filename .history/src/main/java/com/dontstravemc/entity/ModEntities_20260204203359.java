package com.dontstravemc.entity;


import com.dontstrave;
import com.dontstravemc.entity.animal.ButterflyEntity;
import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import com.dontstravemc.entity.monster.SpiderDenEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final ResourceKey<EntityType<?>> MONSTER_SPIDER_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            dontstrave.MOD_ID,
                            "monster_spider"
                    )
            );

    public static final EntityType<MonsterSpiderEntity> MONSTER_SPIDER =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    MONSTER_SPIDER_KEY,
                    EntityType.Builder.of(MonsterSpiderEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 0.9F)
                            .build(MONSTER_SPIDER_KEY)
            );

    public static final ResourceKey<EntityType<?>> BUTTERFLY_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            dontstrave.MOD_ID,
                            "butterfly"
                    )
            );

    public static final EntityType<ButterflyEntity> BUTTERFLY =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    BUTTERFLY_KEY,
                    EntityType.Builder.of(ButterflyEntity::new, MobCategory.AMBIENT)
                            .sized(0.5F, 0.5F) // 设置蝴蝶碰撞箱大小
                            .build(BUTTERFLY_KEY)
            );

    public static final ResourceKey<EntityType<?>> Spider_Den_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            dontstrave.MOD_ID,
                            "spider_den"
                    )
            );

    public static final EntityType<SpiderDenEntity> SPIDER_DEN = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "spider_den"),
            EntityType.Builder.of(SpiderDenEntity::new, MobCategory.MONSTER)
                    .sized(1.5f, 1.5f) // 设置碰撞箱大小：宽2米，高2米（根据你的模型调整）
                    .build(Spider_Den_KEY)
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(
                MONSTER_SPIDER,
                MonsterSpiderEntity.createSpiderAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                BUTTERFLY,
                ButterflyEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                ModEntities.SPIDER_DEN, // 你的实体类型
                SpiderDenEntity.createSpiderDenAttributes() // 你的属性构建器
        );
    }
}
