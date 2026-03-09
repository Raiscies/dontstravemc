package com.dontstravemc.block.custom;

import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Waxed Sapling: obtained by digging a sapling with a shovel.
 * When placed, defaults to HARVESTED=true (no twigs, directly enters regrowth timer).
 * All other logic inherited from SaplingBlock.
 */
public class WaxedSaplingBlock extends SaplingBlock {
    public static final MapCodec<WaxedSaplingBlock> CODEC = simpleCodec(WaxedSaplingBlock::new);

    public WaxedSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Waxed sapling starts in harvested state (directly enters regrowth)
        this.registerDefaultState(this.stateDefinition.any().setValue(HARVESTED, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
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
            // With shovel: drop waxed sapling (itself)
            drops.add(new ItemStack(ModBlocks.SAPLING_WAXED.asItem(), 1));
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
