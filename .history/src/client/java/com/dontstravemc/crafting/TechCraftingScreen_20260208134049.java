package com.dontstravemc.crafting;

import com.dontstravemc.crafting.networking.CraftRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final String CATEGORY_TEXTURE_PATH = "textures/gui/tech_crafting/categories/";
    private static final ResourceLocation CATEGORY_BG_ATLAS = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/categories/category_bg.png");

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
            int rectY = y + (i * 22); // Re-spaced for 20px height
            boolean hovered = mouseX >= x && mouseX <= x + 22 && mouseY >= rectY && mouseY <= rectY + 20;
            
            // Check if this category has recipes
            boolean hasRecipes = TechRecipeManager.INSTANCE.getRecipesByCategory(cat).size() > 0;
            
            // 1. Draw Availability Background
            // No craftable (x4,y4 to x21,y21) = 18x18
            // Has craftable (x4,y26 to x21,y43) = 18x18
            int bgU = 4;
            int bgV = hasRecipes ? 26 : 4;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CATEGORY_BG_ATLAS, x + 2, rectY + 1, bgU, bgV, 18, 18, 256, 256);

            // 2. Draw Theme Frame
            // Default (x2,y48 to x23,y67) = 22x20
            // Hovered (x2,y72 to x23,y91) = 22x20
            // Selected (x2,y96 to x23,y115) = 22x20
            int frameV = 48; // Default
            if (cat == selectedCategory) {
                frameV = 96; // Selected
            } else if (hovered) {
                frameV = 72; // Hovered
            }
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CATEGORY_BG_ATLAS, x, rectY, 2, frameV, 22, 20, 256, 256);

            // 3. Render category icon (16x16, centered)
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath("dontstravemc", CATEGORY_TEXTURE_PATH + cat.getId() + ".png");
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, iconTexture, x + 3, rectY + 2, 0, 0, 16, 16, 16, 16);
            
            // Render tooltip on hover
            if (hovered) {
                guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("category.dontstravemc." + cat.getId()), mouseX, mouseY);
            }
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
        int catX = x - 30; // Shifting a bit to avoid overlap with main window
        TechCategory[] categories = TechCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int rectY = y + 10 + (i * 22);
            if (mouseX >= catX && mouseX <= catX + 22 && mouseY >= rectY && mouseY <= rectY + 20) {
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
