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
        // UI layout initialization
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;

        // Draw basic background (using colors if texture missing)
        guiGraphics.fill(x, y, x + 256, y + 200, 0xBB000000); // Dark semi-transparent
        guiGraphics.renderOutline(x, y, 256, 200, 0xFFAAAAAA);

        // Render Categories
        renderCategories(guiGraphics, x - 30, y + 10, mouseX, mouseY);

        // Render Recipe List
        renderRecipeList(guiGraphics, x + 10, y + 10, mouseX, mouseY);

        // Render Selected Recipe Info
        if (selectedRecipe != null) {
            renderRecipeDetails(guiGraphics, x + 130, y + 10, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderCategories(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        TechCategory[] categories = TechCategory.values();
        for (int i = 0; i < categories.length; i++) {
            TechCategory cat = categories[i];
            int rectY = y + (i * 22);
            boolean hovered = mouseX >= x && mouseX <= x + 30 && mouseY >= rectY && mouseY <= rectY + 20;
            int color = (cat == selectedCategory) ? 0xFFFFFFFF : (hovered ? 0xFFAAAAAA : 0xFF666666);
            
            guiGraphics.fill(x, rectY, x + 30, rectY + 20, color);
            guiGraphics.drawString(this.font, cat.name().substring(0, 1), x + 12, rectY + 6, 0x000000, false);
        }
    }

    private void renderRecipeList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        List<TechRecipe> recipes = TechRecipeManager.INSTANCE.getRecipesByCategory(selectedCategory);
        for (int i = 0; i < Math.min(recipes.size(), 8); i++) {
            TechRecipe recipe = recipes.get(i);
            int rectY = y + (i * 22);
            boolean hovered = mouseX >= x && mouseX <= x + 110 && mouseY >= rectY && mouseY <= rectY + 20;
            
            if (recipe == selectedRecipe) {
                guiGraphics.fill(x, rectY, x + 110, rectY + 20, 0x66FFFFFF);
            } else if (hovered) {
                guiGraphics.fill(x, rectY, x + 110, rectY + 20, 0x33FFFFFF);
            }
            
            guiGraphics.renderFakeItem(recipe.getResult(), x + 2, rectY + 2);
            guiGraphics.drawString(this.font, recipe.getResult().getHoverName(), x + 22, rectY + 6, 0xFFFFFF);
        }
    }

    private void renderRecipeDetails(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, selectedRecipe.getResult().getHoverName(), x, y, 0xFFFFFF);
        guiGraphics.renderItem(selectedRecipe.getResult(), x, y + 15);
        
        // Render ingredients
        int iy = y + 60;
        guiGraphics.drawString(this.font, "Ingredients:", x, iy, 0xAAAAAA);
        int j = 0;
        for (Map.Entry<Ingredient, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            ItemStack[] stacks = entry.getKey().getItems();
            if (stacks.length > 0) {
                guiGraphics.renderItem(stacks[0], x, iy + 15 + (j * 20));
                guiGraphics.drawString(this.font, "x" + entry.getValue(), x + 20, iy + 20 + (j * 20), 0xFFFFFF);
                j++;
            }
        }

        // Craft Button
        int btnX = x;
        int btnY = y + 160;
        boolean hovered = mouseX >= btnX && mouseX <= btnX + 100 && mouseY >= btnY && mouseY <= btnY + 25;
        guiGraphics.fill(btnX, btnY, btnX + 100, btnY + 25, hovered ? 0xFF00AA00 : 0xFF008800);
        guiGraphics.drawCenteredString(this.font, "CRAFT", btnX + 50, btnY + 8, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;

        // Category clicks
        int catX = x - 30;
        TechCategory[] categories = TechCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int rectY = y + 10 + (i * 22);
            if (mouseX >= catX && mouseX <= catX + 30 && mouseY >= rectY && mouseY <= rectY + 20) {
                selectedCategory = categories[i];
                selectedRecipe = null;
                return true;
            }
        }

        // Recipe clicks
        int listX = x + 10;
        List<TechRecipe> recipes = TechRecipeManager.INSTANCE.getRecipesByCategory(selectedCategory);
        for (int i = 0; i < Math.min(recipes.size(), 8); i++) {
            int rectY = y + 10 + (i * 22);
            if (mouseX >= listX && mouseX <= listX + 110 && mouseY >= rectY && mouseY <= rectY + 20) {
                selectedRecipe = recipes.get(i);
                return true;
            }
        }

        // Craft Button click
        if (selectedRecipe != null) {
            int btnX = x + 130;
            int btnY = y + 170;
            if (mouseX >= btnX && mouseX <= btnX + 100 && mouseY >= btnY && mouseY <= btnY + 25) {
                ClientPlayNetworking.send(new CraftRequestPayload(selectedRecipe.getId()));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
