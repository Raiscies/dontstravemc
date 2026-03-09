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
    private static final ResourceLocation DETAILS_BG = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/background_2.png");

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Total layout width: 178 (list) + 20 (gap) + 131 (details) = 329
        int totalWidth = 178 + 20 + 131;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - 185) / 2;

        // 1. Draw List Background (178x185)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_BG, x, y, 0, 0, 178, 185, 256, 256);

        // Render Categories (Relative to List BG)
        renderCategories(guiGraphics, x - 25, y + 10, mouseX, mouseY);

        // Render Recipe List (8x8 grid)
        renderRecipeList(guiGraphics, x, y, mouseX, mouseY);

        // Render Selected Recipe Info
        if (selectedRecipe != null) {
            // 2. Draw Details Background (131x165), centered vertically to the list
            int dx = x + 178 + 20;
            int dy = y + (185 - 165) / 2;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DETAILS_BG, dx, dy, 0, 0, 131, 165, 256, 256);
            
            renderRecipeDetails(guiGraphics, dx, dy, mouseX, mouseY);
        }
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
        // Grid: 8x8 starting at x19, y26. X-pitch: 18, Y-pitch: 17
        for (int i = 0; i < Math.min(recipes.size(), 64); i++) {
            TechRecipe recipe = recipes.get(i);
            int row = i / 8;
            int col = i % 8;
            int rx = x + 19 + (col * 18);
            int ry = y + 26 + (row * 17);
            
            boolean hovered = mouseX >= rx && mouseX <= rx + 18 && mouseY >= ry && mouseY <= ry + 18;
            
            if (recipe == selectedRecipe) {
                guiGraphics.fill(rx, ry, rx + 18, ry + 18, 0x66FFFFFF);
            } else if (hovered) {
                guiGraphics.fill(rx, ry, rx + 18, ry + 18, 0x33FFFFFF);
            }
            
            // Draw 16x16 icon exactly at slot start to avoid offset
            guiGraphics.renderFakeItem(recipe.getResult(), rx, ry);
        }
    }

    private void renderRecipeDetails(GuiGraphics guiGraphics, int dx, int dy, int mouseX, int mouseY) {
        // dx, dy are the top-left of background_2
        
        // Product Slot at x66, y20
        int px = dx + 66;
        int py = dy + 20;
        guiGraphics.renderFakeItem(selectedRecipe.getResult(), px, py);
        
        // The frame starts at x19 in background_2 atlas and goes to 131?
        // Let's adjust labels to stay within the frame portion (approx dx + 25 to dx + 125)
        int frameX = dx + 20;
        int labelWidth = 100;
        
        // 1. Name Section
        int ny = dy + 45;
        guiGraphics.fill(frameX, ny, frameX + labelWidth, ny + 15, 0xB2000000);
        guiGraphics.drawCenteredString(this.font, selectedRecipe.getResult().getHoverName(), frameX + labelWidth/2, ny + 4, 0xFFFFFFFF);
        
        // 2. Ingredients Section
        int iy = dy + 65;
        guiGraphics.fill(frameX, iy, frameX + labelWidth, iy + 25, 0xB2000000);
        int j = 0;
        for (Map.Entry<Ingredient, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            ItemStack[] stacks = entry.getKey().items().map(h -> new ItemStack(h.value())).toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                int itemX = frameX + 5 + (j * 20);
                int itemY = iy + 4;
                ItemStack displayStack = stacks[0].copy();
                displayStack.setCount(entry.getValue());
                guiGraphics.renderItem(displayStack, itemX, itemY);
                guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);
                j++;
            }
        }

        // 3. Description Section
        int descY = dy + 95;
        guiGraphics.fill(frameX, descY, frameX + labelWidth, descY + 45, 0xB2000000);
        List<FormattedCharSequence> descLines = this.font.split(Component.translatable(selectedRecipe.getDescriptionKey()), labelWidth - 10);
        for (int i = 0; i < Math.min(descLines.size(), 4); i++) {
            guiGraphics.drawString(this.font, descLines.get(i), frameX + 5, descY + 5 + (i * 10), 0xFFCCCCCC, false);
        }

        // 4. Craft Button
        int btnX = frameX + (labelWidth - 61) / 2;
        int btnY = dy + 145;
        boolean hovered = mouseX >= btnX && mouseX <= btnX + 61 && mouseY >= btnY && mouseY <= btnY + 15;
        guiGraphics.fill(btnX, btnY, btnX + 61, btnY + 15, hovered ? 0xB200AA00 : 0xB2008800);
        guiGraphics.drawCenteredString(this.font, "CRAFT", btnX + 30, btnY + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        
        int totalWidth = 178 + 20 + 131;
        int x = (this.width - totalWidth) / 2;
        int y = (this.height - 185) / 2;
        int dx = x + 178 + 20;
        int dy = y + (185 - 165) / 2;

        // Category clicks
        int catX = x - 25;
        TechCategory[] categories = TechCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int rectY = y + 10 + (i * 22);
            if (mouseX >= catX && mouseX <= catX + 22 && mouseY >= rectY && mouseY <= rectY + 20) {
                selectedCategory = categories[i];
                selectedRecipe = null;
                return true;
            }
        }

        // Recipe clicks (8x8 grid)
        List<TechRecipe> recipes = TechRecipeManager.INSTANCE.getRecipesByCategory(selectedCategory);
        for (int i = 0; i < Math.min(recipes.size(), 64); i++) {
            int row = i / 8;
            int col = i % 8;
            int rx = x + 18 + (col * 18);
            int ry = y + 25 + (row * 18);
            if (mouseX >= rx && mouseX <= rx + 18 && mouseY >= ry && mouseY <= ry + 18) {
                selectedRecipe = recipes.get(i);
                return true;
            }
        }

        // Craft Button click
        if (selectedRecipe != null) {
            int frameX = dx + 20;
            int labelWidth = 100;
            int btnX = frameX + (labelWidth - 61) / 2;
            int btnY = dy + 145;
            if (mouseX >= btnX && mouseX <= btnX + 61 && mouseY >= btnY && mouseY <= btnY + 15) {
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
