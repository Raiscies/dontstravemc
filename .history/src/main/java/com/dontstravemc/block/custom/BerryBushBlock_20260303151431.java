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
 * Natural berry bush block (Don't Starve style).
 * - Naturally generated with stage=3 (full fruit)
 * - Right-click to harvest: drops 1 berry, becomes stage=0
 * - Random Tick regrowth: 3-5 days average, regrows to stage=3
 * - Shovel: drops corresponding waxed berry bush
 * - Non-shovel break: drops nothing
 * - Uses cross model
 */
public class BerryBushBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BerryBushBlock> CODEC = simpleCodec(BerryBushBlock::new);

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    private final int bushType;

    public BerryBushBlock(BlockBehaviour.Properties properties) {
        this(properties, 1);
    }

    public BerryBushBlock(BlockBehaviour.Properties properties, int bushType) {
        super(properties.randomTicks());
        this.bushType = bushType;
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 3));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- Harvest (right-click) ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            int stage = state.getValue(STAGE);
            if (stage > 0) {
                Block.popResource(level, pos, new ItemStack(ModItems.BERRIES, 1));
                level.setBlock(pos, state.setValue(STAGE, 0), 3);
                return InteractionResult.SUCCESS;
            }
        }
        return state.getValue(STAGE) > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    // --- Random tick regrowth (3-5 days average) ---

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) == 0 && random.nextInt(70) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 3), 3);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(STAGE) == 0 || super.isRandomlyTicking(state);
    }

    // --- Bonemeal ---

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(STAGE) == 0;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(STAGE, 3), 3);
    }

    // --- Survival ---

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

        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean usedShovel = tool != null && (
                tool.is(Items.WOODEN_SHOVEL) ||
                tool.is(Items.STONE_SHOVEL) ||
                tool.is(Items.IRON_SHOVEL) ||
                tool.is(Items.GOLDEN_SHOVEL) ||
                tool.is(Items.DIAMOND_SHOVEL) ||
                tool.is(Items.NETHERITE_SHOVEL));

        if (usedShovel) {
            Block waxed = bushType == 1 ? ModBlocks.BERRY_BUSH_WAXED : ModBlocks.BERRY_BUSH_2_WAXED;
            drops.add(new ItemStack(waxed.asItem(), 1));
        }

        return drops;
    }
}
