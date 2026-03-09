package com.dontstravemc.block;

import com.dontstrave;
import com.dontstravemc.block.custom.RadioBlock;
import com.dontstravemc.block.custom.SaplingBlock;
import com.dontstravemc.block.custom.StickyWebbingBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ModBlocks {

    // 1. 定义存储方块对应物品的列表
    public static final List<Item> BLOCKS_FOR_TAB = new ArrayList<>();

    // 示例：注册收音机
    public static final Block RADIO = registerBlock("radio",
            RadioBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(1.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );

    public static final Block STICKY_WEBBING = registerBlock("sticky_webbing",
            // 1. 这里传构造函数引用，而不是 new 对象
            StickyWebbingBlock::new,

            // 2. 这里单独传属性
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .replaceable()
                    .noCollision()
                    .strength(0.5f)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion()
                    .ignitedByLava()
    );

    // Sapling block with cross model
    public static final Block SAPLING = registerBlock("sapling",
            SaplingBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollision()
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
    );

    // Forest turf block
    public static final Block FOREST_TURF = registerBlock("forest_turf",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GRASS)
                    .strength(0.6f)
                    .sound(SoundType.GRASS)
    );



    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, name);

        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        properties.setId(blockKey);
        Block block = factory.apply(properties);

        // 注册方块
        Block registeredBlock = Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        // 2. 注册对应的 BlockItem
        Item blockItem = new BlockItem(registeredBlock, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        // --- 核心修改：将 BlockItem 存入方块列表 ---
        BLOCKS_FOR_TAB.add(blockItem);

        return registeredBlock;
    }

    public static void registerModBlocks() {
        dontstrave.LOGGER.info("Registering Blocks for " + dontstrave.MOD_ID);
    }
}