package com.dontstravemc.crafting;

import com.dontstravemc.crafting.networking.CraftRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public class TechCraftingScreen extends Screen {
    
    private TechCategory selectedCategory = TechCategory.TOOLS;
    private TechRecipe selectedRecipe = null;

    public TechCraftingScreen() {
        super(Component.translatable("gui.dontstravemc.tech_crafting"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.10, renderBackground is called automatically by renderWithTooltipAndSubtitles
        // Calling it here causes "Can only blur once per frame" crash.
        
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;

        // Draw basic background
        guiGraphics.fill(x, y, x + 256, y + 200, 0xBB000000); // Dark semi-transparent
        
        // Draw Outlines manually using hLine/vLine or multiple fill
        guiGraphics.fill(x, y, x + 256, y + 1, 0xFFAAAAAA); // Top
        guiGraphics.fill(x, y + 199, x + 256, y + 200, 0xFFAAAAAA); // Bottom
        guiGraphics.fill(x, y, x + 1, y + 200, 0xFFAAAAAA); // Left
        guiGraphics.fill(x + 255, y, x + 256, y + 200, 0xFFAAAAAA); // Right

        // Render Categories
        renderCategories(guiGraphics, x - 35, y + 10, mouseX, mouseY);

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
            int rectY = y + (i * 18);
            boolean hovered = mouseX >= x && mouseX <= x + 30 && mouseY >= rectY && mouseY <= rectY + 16;
            int color = (cat == selectedCategory) ? 0xFFFFFFFF : (hovered ? 0xFFAAAAAA : 0xFF666666);
            
            guiGraphics.fill(x, rectY, x + 30, rectY + 16, color);
            String label = cat.name().substring(0, 1);
            guiGraphics.drawString(this.font, label, x + 12, rectY + 4, 0x000000, false);
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
        guiGraphics.renderFakeItem(selectedRecipe.getResult(), x, y + 15);
        
        // Render ingredients
        int iy = y + 50;
        guiGraphics.drawString(this.font, Component.translatable("gui.dontstravemc.ingredients"), x, iy, 0xAAAAAA);
        int j = 0;
        for (Map.Entry<Ingredient, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            ItemStack[] stacks = entry.getKey().items().map(h -> new ItemStack(h.value())).toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                ItemStack displayStack = stacks[0].copy();
                int amount = entry.getValue();
                displayStack.setCount(amount);
                
                int itemX = x;
                int itemY = iy + 15 + (j * 20);
                
                guiGraphics.renderItem(displayStack, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);
                guiGraphics.drawString(this.font, "x" + amount, itemX + 22, itemY + 5, 0xFFFFFF);
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
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;

        // Category clicks
        int catX = x - 35;
        TechCategory[] categories = TechCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int rectY = y + 10 + (i * 18);
            if (mouseX >= catX && mouseX <= catX + 30 && mouseY >= rectY && mouseY <= rectY + 16) {
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

        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
