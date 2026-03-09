package com.dontstravemc.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for blocks that can be fertilized.
 * Any block implementing this interface will automatically work with
 * all registered fertilizer items through FertilizerHelper.
 */
public interface Fertilizable {
    /**
     * Check if this block can currently be fertilized.
     */
    boolean canBeFertilized(BlockState state, Level level, BlockPos pos);

    /**
     * Apply fertilization effect to this block.
     */
    void fertilize(BlockState state, Level level, BlockPos pos, Player player);
}
