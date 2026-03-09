package com.dontstravemc.crafting;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public interface KnowledgeComponents extends AutoSyncedComponent {
    boolean isRecipeUnlocked(ResourceLocation recipeId);
    void unlockRecipe(ResourceLocation recipeId);
    Set<ResourceLocation> getUnlockedRecipes();
    
    int getTechLevel();
    void setTechLevel(int level);

    @Override void readData(ValueInput input);
    @Override void writeData(ValueOutput output);
}
