package com.dontstravemc.dsfood.custom;

import com.dontstravemc.status.ModComponents; // 假设你的组件注册在这个包下
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SanityFoodItem extends Item {
    private final float sanityModifier;

    public SanityFoodItem(Properties properties, float sanityModifier) {
        super(properties);
        this.sanityModifier = sanityModifier;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        // 仅在服务端处理逻辑，且目标必须是玩家
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            // 通过 CCA 获取理智值组件并修改
            // 注意：请根据你实际的组件获取方式修改以下代码
            ModComponents.SANITY.get(player).addSanity(sanityModifier);
        }

        return result;
    }
}
