package com.dontstravemc.dsfood;

import net.minecraft.world.food.FoodProperties;

//在这个文件注册食物类物品的食物基本能力
public class Dsfoods {

    public static final FoodProperties MONSTER_MEAT_FOOD =
            new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.1f)
                    .alwaysEdible()
                    .build();

    public static final FoodProperties COOKED_MONSTER_MEAT_FOOD =
            new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.3f)
                    .alwaysEdible()
                    .build();

    public static final FoodProperties MONSTER_MEAT_JERKY_FOOD =
            new FoodProperties.Builder()
                    .nutrition(6)
                    .saturationModifier(0.6f)
                    .alwaysEdible()
                    .build();
    public static final FoodProperties BUTTERFLY_WINGS_FOOD =
            new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0f)
                    .alwaysEdible()
                    .build();
    public static final FoodProperties BUTTER_FOOD =
            new FoodProperties.Builder()
                    .nutrition(8)
                    .saturationModifier(1f)
                    .alwaysEdible()
                    .build();

    // 腐烂食物：基础食物属性（不提供正向饱食度；负向饱食度在 Item.finishUsingItem 里实现）
    public static final FoodProperties SPOILED_FOOD =
            new FoodProperties.Builder()
                    .nutrition(0)
                    .saturationModifier(0f)
                    .alwaysEdible()
                    .build();

    // 浆果：恢复1点饱食度
    public static final FoodProperties BERRIES_FOOD =
            new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.1f)
                    .build();

    // 烤浆果：恢复2点饱食度
    public static final FoodProperties COOKED_BERRIES_FOOD =
            new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.3f)
                    .build();
}

