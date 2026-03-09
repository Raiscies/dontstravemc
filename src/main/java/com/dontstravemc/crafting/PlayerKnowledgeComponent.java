package com.dontstravemc.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.dontstravemc.status.ModComponents;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class PlayerKnowledgeComponent implements KnowledgeComponents, AutoSyncedComponent {
    private final Player player;
    private final Set<ResourceLocation> unlockedRecipes = new HashSet<>();
    private int techLevel = 0;

    public PlayerKnowledgeComponent(Player player) {
        this.player = player;
    }

    @Override
    public boolean isRecipeUnlocked(ResourceLocation recipeId) {
        return unlockedRecipes.contains(recipeId);
    }

    @Override
    public void unlockRecipe(ResourceLocation recipeId) {
        if (unlockedRecipes.add(recipeId)) {
            ModComponents.KNOWLEDGE.sync(this.player);
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
        ModComponents.KNOWLEDGE.sync(this.player);
    }

    @Override
    public void readData(ValueInput input) {
        if (input instanceof net.minecraft.world.level.storage.TagValueInput tagInput) {
            this.techLevel = tagInput.getIntOr("TechLevel", 0);
            this.unlockedRecipes.clear();
            int count = tagInput.getIntOr("RecipeCount", 0);
            for (int i = 0; i < count; i++) {
                String id = tagInput.getStringOr("Recipe_" + i, "");
                if (!id.isEmpty()) {
                    unlockedRecipes.add(ResourceLocation.parse(id));
                }
            }
        }
    }

    @Override
    public void writeData(ValueOutput output) {
        if (output instanceof net.minecraft.world.level.storage.TagValueOutput tagOutput) {
            tagOutput.putInt("TechLevel", techLevel);
            tagOutput.putInt("RecipeCount", unlockedRecipes.size());
            int i = 0;
            for (ResourceLocation id : unlockedRecipes) {
                tagOutput.putString("Recipe_" + i, id.toString());
                i++;
            }
        }
    }

    // --- 网络同步 ---
    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeInt(this.techLevel);
        buf.writeInt(unlockedRecipes.size());
        for (ResourceLocation id : unlockedRecipes) {
            buf.writeResourceLocation(id);
        }
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.techLevel = buf.readInt();
        this.unlockedRecipes.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            unlockedRecipes.add(buf.readResourceLocation());
        }
    }
}
