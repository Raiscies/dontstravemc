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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Twigs item that can be placed on the ground to become a Twig block.
 * Right-click on a valid surface to place.
 */
public class PlaceableTwigsItem extends Item {
    public PlaceableTwigsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().relative(face);

        // Only place on top of blocks
        if (face != Direction.UP) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockState twigState = ModBlocks.TWIG.defaultBlockState();

            // Check if the position is replaceable (air or similar)
            if (level.getBlockState(placePos).canBeReplaced()) {
                // Place the twig block facing the player's direction
                Direction playerFacing = context.getHorizontalDirection().getOpposite();
                twigState = twigState.setValue(
                        com.dontstravemc.block.custom.TwigBlock.FACING, playerFacing);

                level.setBlock(placePos, twigState, 3);
                level.playSound(null, placePos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

                // Consume the item (unless creative)
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
