package com.dontstravemc.block.custom;

import com.dontstravemc.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SaplingBlock extends Block {
    public static final MapCodec<SaplingBlock> CODEC = simpleCodec(SaplingBlock::new);
    
    // Use a boolean property to track whether the sapling has been harvested
    public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");

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
}
