package com.dontstravemc.crafting.networking;

import com.dontstravemc.crafting.TechRecipe;
import com.dontstravemc.crafting.TechRecipeManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.dontstravemc.status.ModComponents;
import java.util.Map;
import java.util.Optional;

public class ModNetworking {
    public static void registerPackets() {
        // Register Payload Types
        PayloadTypeRegistry.playC2S().register(CraftRequestPayload.TYPE, CraftRequestPayload.CODEC);

        // Register C2S crafting request
        ServerPlayNetworking.registerGlobalReceiver(CraftRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                handleCraftRequest(context.player(), payload.recipeId());
            });
        });
    }

    private static void handleCraftRequest(ServerPlayer player, ResourceLocation recipeId) {
        // Find recipe in manager
        Optional<TechRecipe> recipeEntry = TechRecipeManager.INSTANCE.getRecipes().stream()
                .filter(r -> r.getId().equals(recipeId))
                .findFirst();

        if (recipeEntry.isEmpty()) return;
        TechRecipe recipe = recipeEntry.get();

        // 1. Validate tech level
        int playerTech = ModComponents.KNOWLEDGE.get(player).getTechLevel();
        if (playerTech < recipe.getRequiredTechLevel()) return;

        // 2. Check ingredients
        if (canCraft(player, recipe)) {
            consumeIngredients(player, recipe);
            player.getInventory().add(recipe.getResult().copy());
            // 3. Mark as discovered (Unlock)
            ModComponents.KNOWLEDGE.get(player).unlockRecipe(recipeId);
        }
    }

    private static boolean canCraft(Player player, TechRecipe recipe) {
        for (Map.Entry<Ingredient, Integer> entry : recipe.getIngredients().entrySet()) {
            int count = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (entry.getKey().test(stack)) {
                    count += stack.getCount();
                }
            }
            if (count < entry.getValue()) return false;
        }
        return true;
    }

    private static void consumeIngredients(Player player, TechRecipe recipe) {
        for (Map.Entry<Ingredient, Integer> entry : recipe.getIngredients().entrySet()) {
            int toRemove = entry.getValue();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (entry.getKey().test(stack)) {
                    int taken = Math.min(stack.getCount(), toRemove);
                    stack.shrink(taken);
                    toRemove -= taken;
                    if (toRemove <= 0) break;
                }
            }
        }
    }
}
