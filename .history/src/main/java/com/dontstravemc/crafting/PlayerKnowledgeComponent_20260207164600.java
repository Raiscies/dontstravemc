package com.dontstravemc.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.HashSet;
import java.util.Set;

public class PlayerKnowledgeComponent implements KnowledgeComponents {
    private final Set<ResourceLocation> unlockedRecipes = new HashSet<>();
    private int techLevel = 0;

    public PlayerKnowledgeComponent(Player player) {
    }

    @Override
    public boolean isRecipeUnlocked(ResourceLocation recipeId) {
        return unlockedRecipes.contains(recipeId);
    }

    @Override
    public void unlockRecipe(ResourceLocation recipeId) {
        if (unlockedRecipes.add(recipeId)) {
            // Logic for sanity recovery on first unlock could go here
        }
    }

    @Override
    public Set<ResourceLocation> getUnlockedRecipes() {
        return new HashSet<>(unlockedRecipes);
    }

    @Override
    public int getTechLevel() {
        return techLevel;
    }

    @Override
    public void setTechLevel(int level) {
        this.techLevel = level;
    }

    @Override
    public void readData(ValueInput input) {
        this.techLevel = input.getIntOr("TechLevel", 0);
        this.unlockedRecipes.clear();
        int count = input.getIntOr("RecipeCount", 0);
        for (int i = 0; i < count; i++) {
            String id = input.getStringOr("Recipe_" + i, "");
            if (!id.isEmpty()) {
                unlockedRecipes.add(ResourceLocation.parse(id));
            }
        }
    }

    @Override
    public void writeData(ValueOutput output) {
        output.putInt("TechLevel", techLevel);
        output.putInt("RecipeCount", unlockedRecipes.size());
        int i = 0;
        for (ResourceLocation id : unlockedRecipes) {
            output.putString("Recipe_" + i, id.toString());
            i++;
        }
    }
}
