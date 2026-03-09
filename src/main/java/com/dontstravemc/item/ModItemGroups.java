package com.dontstravemc.item;

import com.dontstravemc.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

// 在本文件中增加了饥荒物品栏，并添加物品
public class ModItemGroups {

    public static final CreativeModeTab DONTSTRAVE_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "main"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.dontstravemc.main"))
                    .icon(() -> new ItemStack(ModItems.MONSTER_MEAT))
                    .displayItems((context, entries) -> {
                        // 3. 自动遍历列表并添加物品
                        ModItems.ITEMS_FOR_TAB.forEach(entries::accept);
                    })
                    .build()
    );

    public static final CreativeModeTab DONTSTRAVE_BLOCKS_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "blocks"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.dontstravemc.blocks"))
                    // 使用收音机作为图标
                    .icon(() -> new ItemStack(ModBlocks.RADIO))
                    .displayItems((context, entries) -> {
                        // 遍历并添加方块列表
                        ModBlocks.BLOCKS_FOR_TAB.forEach(entries::accept);
                    })
                    .build()
    );

    public static void init() {
    }
}
