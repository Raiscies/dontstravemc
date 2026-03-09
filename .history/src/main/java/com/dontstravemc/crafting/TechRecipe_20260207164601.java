package com.dontstravemc.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import java.util.Map;

public class TechRecipe {
    private final ResourceLocation id;
    private final TechCategory category;
    private final ItemStack result;
    private final Map<Ingredient, Integer> ingredients;
    private final int requiredTechLevel;
    private final String descriptionKey;

    public TechRecipe(ResourceLocation id, TechCategory category, ItemStack result, Map<Ingredient, Integer> ingredients, int requiredTechLevel, String descriptionKey) {
        this.id = id;
        this.category = category;
        this.result = result;
        this.ingredients = ingredients;
        this.requiredTechLevel = requiredTechLevel;
        this.descriptionKey = descriptionKey;
    }

    public ResourceLocation getId() { return id; }
    public TechCategory getCategory() { return category; }
    public ItemStack getResult() { return result.copy(); }
    public Map<Ingredient, Integer> getIngredients() { return ingredients; }
    public int getRequiredTechLevel() { return requiredTechLevel; }
    public String getDescriptionKey() { return descriptionKey; }
}
