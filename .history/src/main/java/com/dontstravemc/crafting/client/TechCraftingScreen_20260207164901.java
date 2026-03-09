package com.dontstravemc.crafting.client;

import com.dontstravemc.crafting.TechCategory;
import com.dontstravemc.crafting.TechRecipe;
import com.dontstravemc.crafting.TechRecipeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class TechCraftingScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/crafting_bg.png");
    
    private TechCategory selectedCategory = TechCategory.TOOLS;
    private TechRecipe selectedRecipe = null;
    private List<TechRecipe> currentRecipes;

    public TechCraftingScreen() {
        super(Component.translatable("gui.dontstravemc.tech_crafting"));
        updateRecipeList();
    }

    private void updateRecipeList() {
        this.currentRecipes = TechRecipeManager.INSTANCE.getRecipesByCategory(selectedCategory);
        if (!currentRecipes.isEmpty() && (selectedRecipe == null || selectedRecipe.getCategory() != selectedCategory)) {
            selectedRecipe = currentRecipes.get(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        // Here we will add buttons for categories
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // Draw main background (placeholder for now)
        int x = (this.width - 250) / 2;
        int y = (this.height - 180) / 2;
        guiGraphics.fill(x, y, x + 250, y + 180, 0xAA000000); // Semi-transparent black

        // Left sidebar (Categories)
        guiGraphics.fill(x, y, x + 40, y + 180, 0x44FFFFFF); 
        
        // Middle (Item List)
        guiGraphics.fill(x + 40, y, x + 150, y + 180, 0x22FFFFFF);

        // Right (Details)
        if (selectedRecipe != null) {
            guiGraphics.renderItem(selectedRecipe.getResult(), x + 160, y + 20);
            guiGraphics.drawString(this.font, selectedRecipe.getResult().getHoverName(), x + 180, y + 25, 0xFFFFFF);
            // Render ingredients...
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Logic for clicking categories and items
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
