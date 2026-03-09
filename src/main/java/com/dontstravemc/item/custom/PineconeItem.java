package com.dontstravemc.item.custom;

import com.dontstravemc.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PineconeItem extends Item {
    public PineconeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(pos);
        BlockPos placePos = pos;
        
        // If clicking on a solid block, place on top
        if (clickedState.isSolid() && context.getClickedFace() == Direction.UP) {
            placePos = pos.above();
        }
        
        // Check if the position is air and the block below is suitable
        if (level.getBlockState(placePos).isAir()) {
            BlockState belowState = level.getBlockState(placePos.below());
            
            // Can plant on dirt, grass, podzol, coarse dirt, rooted dirt, or forest turf
            if (belowState.is(Blocks.DIRT) || 
                belowState.is(Blocks.GRASS_BLOCK) || 
                belowState.is(Blocks.PODZOL) ||
                belowState.is(Blocks.COARSE_DIRT) ||
                belowState.is(Blocks.ROOTED_DIRT) ||
                belowState.is(ModBlocks.FOREST_TURF)) {
                
                if (!level.isClientSide()) {
                    // Place the evergreen sapling
                    level.setBlock(placePos, ModBlocks.EVERGREEN_SAPLING.defaultBlockState(), 3);
                    
                    // Play planting sound
                    level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    
                    // Consume the pinecone
                    context.getItemInHand().shrink(1);
                }
                
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
        }
        
        return InteractionResult.PASS;
    }
}
