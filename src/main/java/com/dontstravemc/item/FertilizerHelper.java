package com.dontstravemc.item;

import com.dontstravemc.block.Fertilizable;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Universal fertilizer system.
 * Register any item as a fertilizer, and it will automatically work
 * on any block implementing the Fertilizable interface.
 *
 * Usage:
 *   FertilizerHelper.registerFertilizer(myItem);
 */
public class FertilizerHelper {

    private static final Set<Item> FERTILIZERS = new HashSet<>();

    /**
     * Register an item as a fertilizer.
     */
    public static void registerFertilizer(Item item) {
        FERTILIZERS.add(item);
    }

    /**
     * Check if the given item stack is a registered fertilizer.
     */
    public static boolean isFertilizer(ItemStack stack) {
        return !stack.isEmpty() && FERTILIZERS.contains(stack.getItem());
    }

    /**
     * Register all default fertilizers and set up the UseBlockCallback.
     * Call this in your mod initializer.
     */
    public static void registerFertilizers() {
        // Register default fertilizers
        registerFertilizer(Items.BONE_MEAL);
        registerFertilizer(ModItems.SPOILED_FOOD);

        // Global right-click block callback for fertilizing
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack heldItem = player.getItemInHand(hand);
            if (!isFertilizer(heldItem)) {
                return InteractionResult.PASS;
            }

            BlockState state = world.getBlockState(hitResult.getBlockPos());
            Block block = state.getBlock();

            if (block instanceof Fertilizable fertilizable) {
                if (fertilizable.canBeFertilized(state, world, hitResult.getBlockPos())) {
                    fertilizable.fertilize(state, world, hitResult.getBlockPos(), player);

                    // Consume the fertilizer item (unless creative)
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }

                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        });
    }
}
