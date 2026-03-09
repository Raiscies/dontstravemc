package com.dontstravemc.crafting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TechRecipeManager implements SimpleResourceReloader<Map<ResourceLocation, TechRecipe>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private Map<ResourceLocation, TechRecipe> recipes = new HashMap<>();
    
    public static final TechRecipeManager INSTANCE = new TechRecipeManager();

    private TechRecipeManager() {}

    public Collection<TechRecipe> getRecipes() {
        return recipes.values();
    }

    public List<TechRecipe> getRecipesByCategory(TechCategory category) {
        List<TechRecipe> categoryRecipes = new ArrayList<>();
        for (TechRecipe recipe : recipes.values()) {
            if (recipe.getCategory() == category) {
                categoryRecipes.add(recipe);
            }
        }
        return categoryRecipes;
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath("dontstravemc", "tech_recipes");
    }

    @Override
    public CompletableFuture<Map<ResourceLocation, TechRecipe>> load(ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, TechRecipe> loadedRecipes = new HashMap<>();
            Map<ResourceLocation, Resource> resources = manager.listResources("tech_recipes", id -> id.getPath().endsWith(".json"));
            
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation id = entry.getKey();
                // Convert dontstravemc:tech_recipes/axe.json -> dontstravemc:axe
                String path = id.getPath().substring("tech_recipes/".length(), id.getPath().length() - ".json".length());
                ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);

                try (Reader reader = new InputStreamReader(entry.getValue().open())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    TechRecipe recipe = deserialize(recipeId, json);
                    loadedRecipes.put(recipeId, recipe);
                } catch (Exception e) {
                    LOGGER.error("Failed to load tech recipe {}: {}", id, e.getMessage());
                }
            }
            return loadedRecipes;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<ResourceLocation, TechRecipe> data, ResourceManager manager, ProfilerFiller profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            this.recipes = data;
            LOGGER.info("Loaded {} technology recipes", recipes.size());
        }, executor);
    }

    private TechRecipe deserialize(ResourceLocation id, JsonObject json) {
        TechCategory category = TechCategory.valueOf(json.get("category").getAsString().toUpperCase());
        String resultItem = json.get("result").getAsString();
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(resultItem)), count);
        
        int techLevel = json.has("tech_level") ? json.get("tech_level").getAsInt() : 0;
        String description = json.has("description") ? json.get("description").getAsString() : "tooltip.dontstravemc.recipe.no_description";

        Map<Ingredient, Integer> ingredients = new HashMap<>();
        JsonObject ingredientsJson = json.get("ingredients").getAsJsonObject();
        for (String key : ingredientsJson.keySet()) {
            Ingredient ingredient = Ingredient.of(BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(key)));
            int amount = ingredientsJson.get(key).getAsInt();
            ingredients.put(ingredient, amount);
        }

        return new TechRecipe(id, category, result, ingredients, techLevel, description);
    }
}
