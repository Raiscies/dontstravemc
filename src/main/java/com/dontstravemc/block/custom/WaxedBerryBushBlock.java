package com.dontstravemc.block.custom;

import com.dontstravemc.block.Fertilizable;

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
import net.minecraft.world.level.block.BonemealableBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Waxed (transplanted) berry bush block.
 * 
 * Lifecycle:
 * 1. Player plants → withered=true
 * 2. Fertilize → withered=false, stage=0, harvest_count=0
 * 3. Random tick → grows to mature stage (based on harvest_count)
 * 4. Harvest → drops 1 berry, harvest_count++, stage=0
 * 5. Repeat 3-4 until harvest_count >= 6
 * 6. When harvest_count >= 6 → withered=true
 * 
 * Mature stage depends on harvest_count:
 * - 0-1 harvests: stage 3 (most fruit)
 * - 2-3 harvests: stage 2
 * - 4-5 harvests: stage 1 (least fruit)
 * 
 * Growth time: 7 days (140 minutes)
 * Withered state: drops 2 twigs when broken
 */
public class WaxedBerryBushBlock extends Block implements Fertilizable, BonemealableBlock {
    public static final MapCodec<WaxedBerryBushBlock> CODEC = simpleCodec(WaxedBerryBushBlock::new);

    public static final BooleanProperty WITHERED = BooleanProperty.create("withered");
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final IntegerProperty HARVEST_COUNT = IntegerProperty.create("harvest_count", 0, 6);

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    private final int bushType;

    public WaxedBerryBushBlock(BlockBehaviour.Properties properties) {
        this(properties, 1);
    }

    public WaxedBerryBushBlock(BlockBehaviour.Properties properties, int bushType) {
        super(properties.randomTicks());
        this.bushType = bushType;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WITHERED, true)
                .setValue(STAGE, 0)
                .setValue(HARVEST_COUNT, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WITHERED, STAGE, HARVEST_COUNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Compute the mature stage based on harvest count.
     * 0-1 harvests → stage 3 (most fruit)
     * 2-3 harvests → stage 2
     * 4-5 harvests → stage 1 (least fruit)
     */
    private int getMatureStage(int harvestCount) {
        if (harvestCount <= 1) return 3;
        if (harvestCount <= 3) return 2;
        return 1;
    }

    // --- Fertilizable interface (custom fertilizers like Spoiled Food) ---

    @Override
    public boolean canBeFertilized(BlockState state, Level level, BlockPos pos) {
        return state.getValue(WITHERED);
    }

    @Override
    public void fertilize(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            this.performBonemeal(serverLevel, level.random, pos, state);
        }
    }

    // --- BonemealableBlock (Vanilla Bone Meal) ---

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(WITHERED) || (!state.getValue(WITHERED) && state.getValue(STAGE) == 0);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(WITHERED)) {
            // Restore from withered
            level.setBlock(pos, state
                    .setValue(WITHERED, false)
                    .setValue(STAGE, 0)
                    .setValue(HARVEST_COUNT, 0), 3);
        } else if (state.getValue(STAGE) == 0) {
            // Force grow to mature
            int harvestCount = state.getValue(HARVEST_COUNT);
            int matureStage = getMatureStage(harvestCount);
            level.setBlock(pos, state.setValue(STAGE, matureStage), 3);
        }
    }

    // --- Random tick growth (7 days / 140 min) ---

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WITHERED)) {
            return;
        }
        if (state.getValue(STAGE) == 0 && random.nextInt(124) == 0) {
            int harvestCount = state.getValue(HARVEST_COUNT);
            int matureStage = getMatureStage(harvestCount);
            level.setBlock(pos, state.setValue(STAGE, matureStage), 3);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return (!state.getValue(WITHERED) && state.getValue(STAGE) == 0) || super.isRandomlyTicking(state);
    }

    // --- Harvest (right-click) ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (state.getValue(WITHERED)) {
                return InteractionResult.PASS;
            }

            int stage = state.getValue(STAGE);
            if (stage > 0) {
                // Drop 1 berry
                Block.popResource(level, pos, new ItemStack(ModItems.BERRIES, 1));

                int newHarvestCount = state.getValue(HARVEST_COUNT) + 1;
                if (newHarvestCount >= 6) {
                    // Bush withers after 6 harvests
                    level.setBlock(pos, state
                            .setValue(WITHERED, true)
                            .setValue(STAGE, 0)
                            .setValue(HARVEST_COUNT, 0), 3);
                } else {
                    // Ready to regrow
                    level.setBlock(pos, state
                            .setValue(STAGE, 0)
                            .setValue(HARVEST_COUNT, newHarvestCount), 3);
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // --- Survival ---

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        Block belowBlock = belowState.getBlock();
        return belowBlock == Blocks.DIRT ||
               belowBlock == Blocks.GRASS_BLOCK ||
               belowBlock == ModBlocks.FOREST_TURF ||
               belowBlock == Blocks.FARMLAND ||
               belowBlock == Blocks.PODZOL ||
               belowBlock == Blocks.COARSE_DIRT ||
               belowBlock == Blocks.ROOTED_DIRT;
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
        if (isWithered) {
            // Withered bush drops 2 twigs
            drops.add(new ItemStack(ModItems.TWIGS, 2));
            return drops;
        }

        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean usedShovel = tool != null && (
                tool.is(Items.WOODEN_SHOVEL) ||
                tool.is(Items.STONE_SHOVEL) ||
                tool.is(Items.IRON_SHOVEL) ||
                tool.is(Items.GOLDEN_SHOVEL) ||
                tool.is(Items.DIAMOND_SHOVEL) ||
                tool.is(Items.NETHERITE_SHOVEL));

        if (usedShovel) {
            Block selfBlock = bushType == 1 ? ModBlocks.BERRY_BUSH_WAXED : ModBlocks.BERRY_BUSH_2_WAXED;
            drops.add(new ItemStack(selfBlock.asItem(), 1));
        } else {
            // Non-shovel break: 2 twigs
            drops.add(new ItemStack(ModItems.TWIGS, 2));
        }

        return drops;
    }
}
