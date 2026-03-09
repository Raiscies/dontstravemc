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
 * Natural grass block (Don't Starve style).
 * - Right-click to harvest: drops CUT_GRASS, becomes HARVESTED=true
 * - Random Tick regrowth: after some time, regrows to HARVESTED=false
 * - Shovel: drops GRASS_WAXED + CUT_GRASS (if unharvested)
 * - Non-shovel break: drops CUT_GRASS
 * - No growth limit for natural grass
 * Uses tinted_cross model.
 */
public class DsGrassBlock extends Block implements BonemealableBlock {
    public static final MapCodec<DsGrassBlock> CODEC = simpleCodec(DsGrassBlock::new);

    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public DsGrassBlock(BlockBehaviour.Properties properties) {
        super(properties.randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(HARVESTED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HARVESTED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            boolean isHarvested = state.getValue(HARVESTED);

            if (!isHarvested) {
                // Change state to harvested
                level.setBlock(pos, state.setValue(HARVESTED, true), 3);

                // Drop cut grass
                Block.popResource(level, pos, new ItemStack(ModItems.CUT_GRASS, 1));

                return InteractionResult.SUCCESS;
            }
        }

        return state.getValue(HARVESTED) ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }

    /**
     * Random tick growth logic.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HARVESTED) && level.getMaxLocalRawBrightness(pos.above()) >= 0 && random.nextInt(3) == 0) {
            this.performBonemeal(level, random, pos, state);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HARVESTED) || super.isRandomlyTicking(state);
    }

    /**
     * Bonemeal implementation (direct regrowth).
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(HARVESTED);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(HARVESTED, false), 3);
    }

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

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();

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
            // With shovel: always drop grass_waxed
            drops.add(new ItemStack(ModBlocks.GRASS_WAXED.asItem(), 1));
            // If unharvested, also drop cut_grass
            if (!isHarvested) {
                drops.add(new ItemStack(ModItems.CUT_GRASS, 1));
            }
        } else {
            // Without shovel: drop cut_grass only (if unharvested)
            if (!isHarvested) {
                drops.add(new ItemStack(ModItems.CUT_GRASS, 1));
            }
            // If already harvested, nothing drops
        }

        return drops;
    }
}
