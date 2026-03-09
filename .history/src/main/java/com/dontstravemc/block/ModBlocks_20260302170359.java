package com.dontstravemc.block;

import com.dontstrave;
import com.dontstravemc.block.custom.DsGrassBlock;
import com.dontstravemc.block.custom.RadioBlock;
import com.dontstravemc.block.custom.SaplingBlock;
import com.dontstravemc.block.custom.StickyWebbingBlock;
import com.dontstravemc.block.custom.TwigBlock;
import com.dontstravemc.block.custom.WaxedGrassBlock;
import com.dontstravemc.block.custom.WaxedSaplingBlock;
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
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;

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

    // Waxed sapling - obtained by digging sapling with shovel, placed starts harvested
    public static final Block SAPLING_WAXED = registerBlock("sapling_waxed",
            WaxedSaplingBlock::new,
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

    // Twig block - GeckoLib static model
    public static final Block TWIG = registerBlock("twig",
            TwigBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .replaceable()
                    .noCollision()
                    .strength(0.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

    // Natural grass (Don't Starve style)
    public static final Block GRASS = registerBlock("grass",
            DsGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollision()
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
    );

    // Waxed grass - transplanted grass (player-placed), has growth limit
    public static final Block GRASS_WAXED = registerBlock("grass_waxed",
            WaxedGrassBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollision()
                    .strength(0.0f)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
    );

    // --- Evergreen Wood Set ---

    public static final Block EVERGREEN_LOG = registerBlock("evergreen_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_WOOD = registerBlock("evergreen_wood",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block STRIPPED_EVERGREEN_LOG = registerBlock("stripped_evergreen_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block STRIPPED_EVERGREEN_WOOD = registerBlock("stripped_evergreen_wood",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_PLANKS = registerBlock("evergreen_planks",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_STAIRS = registerBlock("evergreen_stairs",
            p -> new StairBlock(ModBlocks.EVERGREEN_PLANKS.defaultBlockState(), p),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_SLAB = registerBlock("evergreen_slab",
            SlabBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_FENCE = registerBlock("evergreen_fence",
            FenceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_FENCE_GATE = registerBlock("evergreen_fence_gate",
            p -> new FenceGateBlock(WoodType.SPRUCE, p), // SPRUCE is a placeholder for WoodType
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_PRESSURE_PLATE = registerBlock("evergreen_pressure_plate",
            p -> new PressurePlateBlock(BlockSetType.SPRUCE, p), // SPRUCE placeholder for BlockSetType
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .forceSolidOn()
                    .noCollision()
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
    );

    public static final Block EVERGREEN_BUTTON = registerBlock("evergreen_button",
            p -> new ButtonBlock(BlockSetType.SPRUCE, 30, p),
            BlockBehaviour.Properties.of()
                    .noCollision()
                    .strength(0.5f)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
    );

    public static final Block EVERGREEN_LEAVES = registerBlock("evergreen_leaves",
            p -> new TintedParticleLeavesBlock(0.01f, p),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2f)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
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