package com.dontstravemc.item;

import com.dontstravemc.dsfood.Dsfoods;
import com.dontstrave;
import com.dontstravemc.dsfood.custom.DsFoodItem;
import com.dontstravemc.dsfood.custom.SanityFoodItem;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.dsfood.custom.HealingFoodItem;
import com.dontstravemc.dsfood.custom.MonsterFoodItem;
import com.dontstravemc.item.custom.SpoiledFoodItem;
import com.dontstravemc.item.custom.PlaceableTwigsItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.ArrayList; // 导入 ArrayList
import java.util.List;      // 导入 List
import java.util.function.Function;

public class ModItems {

    // 1. 定义存储所有注册物品的列表
    public static final List<Item> ITEMS_FOR_TAB = new ArrayList<>();
    public static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
        // 保留日志打印（方便以后排查问题）
        dontstrave.LOGGER.info("Registering item: {} with namespace: {}", name, dontstrave.MOD_ID);

        // 生成 ResourceKey
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, name));

        // 创建物品实例
        Item item = itemFactory.apply(settings.setId(itemKey));

        // 执行注册
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        // --- 核心修改：将注册成功的物品存入列表，供物品栏读取 ---
        ITEMS_FOR_TAB.add(item);

        return item;
    }


    public static final Item MONSTER_MEAT = register("monster_meat",
            props -> new DsFoodItem(props, -2.5f, -15.0f, true),
            new Item.Properties().food(Dsfoods.MONSTER_MEAT_FOOD)
    );

    public static final Item COOKED_MONSTER_MEAT = register("cooked_monster_meat",
            props -> new DsFoodItem(props, 0f, -10.0f, true),
            new Item.Properties().food(Dsfoods.COOKED_MONSTER_MEAT_FOOD)
    );

    public static final Item MONSTER_MEAT_JERKY = register("monster_meat_jerky",
            props -> new DsFoodItem(props, 0f, -5.0f, true),
            new Item.Properties().food(Dsfoods.MONSTER_MEAT_JERKY_FOOD)
    );

    public static final Item BUTTER = register("butter",
            props -> new DsFoodItem(props, 5.0f, 0f, false),
            new Item.Properties().food(Dsfoods.BUTTER_FOOD)
    );
    public static final Item BUTTERFLY_WINGS = register("butterfly_wings",
            props -> new DsFoodItem(props, 0f, 0f, false),
            new Item.Properties().food(Dsfoods.BUTTERFLY_WINGS_FOOD)
    );

    // 腐烂食物（负面饱食度 + 可堆肥 + 可燃料）
    public static final Item SPOILED_FOOD = register("spoiled_food",
            SpoiledFoodItem::new,
            new Item.Properties().food(Dsfoods.SPOILED_FOOD)
    );

    // 噩梦燃料（暂时无额外作用，仅作为物品/战利品）
    public static final Item NIGHTMAREFUEL = register("nightmarefuel",
            Item::new,
            new Item.Properties()
    );

    // 树枝（可以合成为木棍，右键地面可放置为树枝方块）
    public static final Item TWIGS = register("twigs",
            PlaceableTwigsItem::new,
            new Item.Properties()
    );

    // 割下的草
    public static final Item CUT_GRASS = register("cut_grass",
            Item::new,
            new Item.Properties()
    );

    // 浆果（恢复1点饱食度）
    public static final Item BERRIES = register("berries",
            Item::new,
            new Item.Properties().food(Dsfoods.BERRIES_FOOD)
    );

    // 烤浆果（恢复2点饱食度）
    public static final Item COOKED_BERRIES = register("cooked_berries",
            Item::new,
            new Item.Properties().food(Dsfoods.ROASTED_BERRIES_FOOD)
    );


/// 刷怪蛋
public static final Item BUTTERFLY_SPAWN_EGG = register("butterfly_spawn_egg",
        SpawnEggItem::new,
        new Item.Properties()
                .component(DataComponents.ENTITY_DATA,
                        TypedEntityData.of(
                                ModEntities.BUTTERFLY,
                                createButterflyNbt()
                        )
                )
);
    public static final Item MONSTER_SPIDER_SPAWN_EGG = register("monster_spider_spawn_egg",
            SpawnEggItem::new,
            new Item.Properties()
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(ModEntities.MONSTER_SPIDER, createMonsterSpiderNbt()))
    );

    public static final Item SPIDER_DEN_SPAWN_EGG = register("spider_den_spawn_egg",
            SpawnEggItem::new,
            new Item.Properties()
                    .component(DataComponents.ENTITY_DATA,
                            TypedEntityData.of(ModEntities.SPIDER_DEN, createSpiderDenNbt()))
    );

    // 辅助方法：构建包含 ID 的 NBT
    private static CompoundTag createButterflyNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "dontstravemc:butterfly");
        return nbt;
    }

    private static CompoundTag createMonsterSpiderNbt() {
        CompoundTag nbt = new CompoundTag();
        // 必须与你注册实体时的 ID 完全一致
        nbt.putString("id", "dontstravemc:monster_spider");
        return nbt;
    }

    private static CompoundTag createSpiderDenNbt() {
        CompoundTag nbt = new CompoundTag();
        // 必须与你注册实体时的 ID 完全一致
        nbt.putString("id", "dontstravemc:spider_den");
        return nbt;
    }

    public static void registerModItems() {
        // 这里的 log 只是为了在控制台看到注册开始的提示
        dontstrave.LOGGER.info("Registering Mod Items for " + dontstrave.MOD_ID);

        // 仅仅通过调用这个方法，Java 就会加载这个类并初始化其中的所有静态常量（也就是执行那些 register 方法）
    }

}