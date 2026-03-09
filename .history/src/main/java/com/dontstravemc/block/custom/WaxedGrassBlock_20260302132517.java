package com.dontstravemc.block.custom;

import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ScheduledTickAccess;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.BonemealableBlock;

/**
 * Waxed (transplanted) grass block.
 * 
 * Lifecycle:
 * 1. Player plants grass_waxed → becomes WITHERED=true (grass_withered texture)
 * 2. Fertilize (Bone Meal or custom) → WITHERED=false, GROWTH_COUNT=20, HARVESTED=true
 * 3. Random Tick → Grows to harvestable (HARVESTED=false, grass texture), GROWTH_COUNT--
 * 4. Harvest → HARVESTED=true (grass_waxed texture)
 * 5. Repeat 3-4 until GROWTH_COUNT=0
 * 6. When GROWTH_COUNT=0 and harvested → WITHERED=true, needs fertilizing again
 * 
 * Withered state drops nothing when broken.
 * Non-withered + shovel → drops grass_waxed block
 * Non-withered + non-shovel + unharvested → drops cut_grass
 */
public class WaxedGrassBlock extends Block implements Fertilizable, BonemealableBlock {
    public static final MapCodec<WaxedGrassBlock> CODEC = simpleCodec(WaxedGrassBlock::new);

    public static final BooleanProperty WITHERED = BooleanProperty.create("withered");
    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");
    public static final IntegerProperty GROWTH_COUNT = IntegerProperty.create("growth_count", 0, 20);

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public WaxedGrassBlock(BlockBehaviour.Properties properties) {
        super(properties.randomTicks());
        // When planted, starts withered
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WITHERED, true)
                .setValue(HARVESTED, true)
                .setValue(GROWTH_COUNT, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WITHERED, HARVESTED, GROWTH_COUNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- Fertilizable interface (for custom fertilizers like Spoiled Food) ---

    @Override
    public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
        // Only withered grass can be customized-fertilized to restore life
        return state.getValue(WITHERED);
    }

    @Override
    public void fertilize(BlockState state, Level level, BlockPos pos, Player player) {
        // Custom fertilize (Spoiled Food) restores withered grass
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            this.performBonemeal(serverLevel, level.random, pos, state);
        }
    }

    // --- BonemealableBlock implementation (for Vanilla Bone Meal) ---

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        // Bone meal works on withered (to restore) or harvested (to grow)
        return state.getValue(WITHERED) || (state.getValue(HARVESTED) && state.getValue(GROWTH_COUNT) > 0);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(WITHERED)) {
            // Restore withered grass
            level.setBlock(pos, state
                    .setValue(WITHERED, false)
                    .setValue(HARVESTED, true)
                    .setValue(GROWTH_COUNT, 20), 3);
        } else if (state.getValue(HARVESTED)) {
            // Force grow harvested grass
            int growthCount = state.getValue(GROWTH_COUNT);
            if (growthCount > 0) {
                level.setBlock(pos, state
                        .setValue(HARVESTED, false)
                        .setValue(GROWTH_COUNT, growthCount - 1), 3);
            }
        }
    }

    // --- Random growth tick ---

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WITHERED)) {
            return; // Withered grass doesn't grow naturally
        }

        // If harvested and has growth cycles left, grow with probability
        if (state.getValue(HARVESTED) && state.getValue(GROWTH_COUNT) > 0) {
            if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
                this.performBonemeal(level, random, pos, state);
            }
        }
    }

    // --- Growth tick ---

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // This tick method is no longer used for growth, randomTick handles it.
        // Keeping it for potential future scheduled tasks, but currently empty.
    }

    // --- Harvest (right-click) ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean isWithered = state.getValue(WITHERED);
            boolean isHarvested = state.getValue(HARVESTED);

            if (isWithered) {
                return InteractionResult.PASS; // Pass to allow Fertilizable/Bonemealable to handle
            }

            if (!isHarvested) {
                int growthCount = state.getValue(GROWTH_COUNT);

                // Drop cut grass
                Block.popResource(level, pos, new ItemStack(ModItems.CUT_GRASS, 1));

                if (growthCount <= 0) {
                    // No more growth cycles → wither
                    level.setBlock(pos, state.setValue(WITHERED, true).setValue(HARVESTED, true).setValue(GROWTH_COUNT, 0), 3);
                } else {
                    // Become harvested
                    level.setBlock(pos, state.setValue(HARVESTED, true), 3);
                }

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    // --- Survival and shape ---

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        Block belowBlock = belowState.getBlock();

        return belowBlock == Blocks.DIRT ||
               belowBlock == Blocks.GRASS_BLOCK ||
               belowBlock == ModBlocks.FOREST_TURF;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    // --- Drops ---

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();

        boolean isWithered = state.getValue(WITHERED);

        // Withered grass drops nothing
        if (isWithered) {
            return drops;
        }

        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean isHarvested = state.getValue(HARVESTED);
        boolean usedShovel = tool != null && (
                tool.is(Items.WOODEN_SHOVEL) ||
                tool.is(Items.STONE_SHOVEL) ||
                tool.is(Items.IRON_SHOVEL) ||
                tool.is(Items.GOLDEN_SHOVEL) ||
                tool.is(Items.DIAMOND_SHOVEL) ||
                tool.is(Items.NETHERITE_SHOVEL));

        if (usedShovel) {
            // Shovel: drop grass_waxed block
            drops.add(new ItemStack(ModBlocks.GRASS_WAXED.asItem(), 1));
            // If unharvested, also drop cut_grass
            if (!isHarvested) {
                drops.add(new ItemStack(ModItems.CUT_GRASS, 1));
            }
        } else {
            // Non-shovel: drop cut_grass if unharvested
            if (!isHarvested) {
                drops.add(new ItemStack(ModItems.CUT_GRASS, 1));
            }
        }

        return drops;
    }
}
