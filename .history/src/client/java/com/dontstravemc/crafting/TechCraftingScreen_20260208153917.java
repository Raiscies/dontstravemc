package com.dontstravemc.crafting;

import com.dontstravemc.crafting.networking.CraftRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
    private static final ResourceLocation MAIN_BG = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/background.png");

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.10, renderBackground is called automatically by renderWithTooltipAndSubtitles
        // Calling it here causes "Can only blur once per frame" crash.
        
        int x = (this.width - 256) / 2;
        int y = (this.height - 200) / 2;

        // Draw basic background texture
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_BG, x, y, 0, 0, 256, 200, 256, 256);

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
        // x and y passed here are the top-left of the main UI (screen x, y)
        
        // Result Item at x217, y20 (relative to background)
        int rx = x + 217;
        int ry = y + 20;
        guiGraphics.renderFakeItem(selectedRecipe.getResult(), rx, ry);
        
        // Name Box (below result)
        int nx = x + 180;
        int ny = y + 45;
        guiGraphics.drawCenteredString(this.font, selectedRecipe.getResult().getHoverName(), x + 217 + 8, ny, 0xFFFFFF);
        
        // Ingredients (below name)
        int iy = y + 65;
        // guiGraphics.drawString(this.font, Component.translatable("gui.dontstravemc.ingredients"), x + 182, iy, 0xAAAAAA);
        int j = 0;
        for (Map.Entry<Ingredient, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            ItemStack[] stacks = entry.getKey().items().map(h -> new ItemStack(h.value())).toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                ItemStack displayStack = stacks[0].copy();
                int amount = entry.getValue();
                displayStack.setCount(amount);
                
                int itemX = x + 185 + (j % 2 * 35);
                int itemY = iy + (j / 2 * 25);
                
                guiGraphics.renderItem(displayStack, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);
                j++;
            }
        }

        // Description (below ingredients)
        int dy = iy + 45;
        List<FormattedCharSequence> descLines = this.font.split(Component.translatable(selectedRecipe.getDescriptionKey()), 70);
        for (int i = 0; i < descLines.size(); i++) {
            guiGraphics.drawString(this.font, descLines.get(i), x + 182, dy + (i * 10), 0xCCCCCC, false);
        }

        // Craft Button (Moved to fit the x178-x255 area or kept in a logical DS position)
        int btnX = x + 185;
        int btnY = y + 145;
        boolean hovered = mouseX >= btnX && mouseX <= btnX + 60 && mouseY >= btnY && mouseY <= btnY + 20;
        guiGraphics.fill(btnX, btnY, btnX + 60, btnY + 20, hovered ? 0xFF00AA00 : 0xFF008800);
        guiGraphics.drawCenteredString(this.font, "CRAFT", btnX + 30, btnY + 6, 0xFFFFFF);
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
