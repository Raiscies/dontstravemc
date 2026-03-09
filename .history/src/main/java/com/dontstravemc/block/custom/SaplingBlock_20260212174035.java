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

import java.util.ArrayList;
import java.util.List;

public class SaplingBlock extends Block {
    public static final MapCodec<SaplingBlock> CODEC = simpleCodec(SaplingBlock::new);
    
    // Use a boolean property to track whether the sapling has been harvested
    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");
    
    // Cross model shape (same as vanilla saplings)
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public SaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Default state: not harvested
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
        // Only process on server side
        if (!level.isClientSide()) {
            // Check if already harvested
            boolean isHarvested = state.getValue(HARVESTED);
            
            if (!isHarvested) {
                // Change state to harvested
                level.setBlock(pos, state.setValue(HARVESTED, true), 3);
                
                // Drop one twigs item
                Block.popResource(level, pos, new ItemStack(ModItems.TWIGS, 1));
                
                return InteractionResult.SUCCESS;
            }
        }
        
        // If already harvested or on client side, return consume to prevent further processing
        return state.getValue(HARVESTED) ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        Block belowBlock = belowState.getBlock();
        
        // Allow placement on dirt, grass block, or forest turf
        return belowBlock == Blocks.DIRT || 
               belowBlock == Blocks.GRASS_BLOCK || 
               belowBlock == ModBlocks.FOREST_TURF;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, BlockPos pos, BlockState neighborState, BlockPos neighborPos, Direction direction) {
        // If the block below is removed, break this block
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Get the tool used
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean isHarvested = state.getValue(HARVESTED);
        boolean usedShovel = tool != null && tool.is(Items.WOODEN_SHOVEL) || 
                             tool != null && tool.is(Items.STONE_SHOVEL) ||
                             tool != null && tool.is(Items.IRON_SHOVEL) ||
                             tool != null && tool.is(Items.GOLDEN_SHOVEL) ||
                             tool != null && tool.is(Items.DIAMOND_SHOVEL) ||
                             tool != null && tool.is(Items.NETHERITE_SHOVEL);
        
        if (usedShovel) {
            // With shovel: always drop 1 sapling
            drops.add(new ItemStack(ModBlocks.SAPLING.asItem(), 1));
            
            // If unharvested, also drop 1 twig
            if (!isHarvested) {
                drops.add(new ItemStack(ModItems.TWIGS, 1));
            }
        } else {
            // Without shovel: drop twigs only
            if (isHarvested) {
                drops.add(new ItemStack(ModItems.TWIGS, 1));
            } else {
                drops.add(new ItemStack(ModItems.TWIGS, 2));
            }
        }
        
        return drops;
    }
}
