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

import com.dontstravemc.status.ModComponents;
import net.minecraft.client.Minecraft;
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
    private static final ResourceLocation ICON_RESEARCHABLE = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/categories/researchable.png");
    private static final ResourceLocation ICON_RESEARCHABLE_OFF = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/categories/researchable_off.png");
    private static final ResourceLocation ICON_LOCK_FRAME = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/categories/lock_itemframe.png");
    private static final ResourceLocation ICON_LOCK = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/tech_crafting/categories/lock.png");

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Total layout width: 178 (list) + 20 (gap) + 147 (details)
        // details = 19 (arrow) + 128 (frame)
        int totalWidth = 178 + 20 + 147;
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
            
            // Check if this category has recipes and prototypes
            List<TechRecipe> catRecipes = TechRecipeManager.INSTANCE.getRecipesByCategory(cat);
            boolean hasRecipes = catRecipes.size() > 0;
            boolean hasPrototypes = false;
            KnowledgeComponents knowledge = ModComponents.KNOWLEDGE.get(Minecraft.getInstance().player);
            for (TechRecipe r : catRecipes) {
                if (knowledge.getTechLevel() >= r.getRequiredTechLevel() && 
                    !knowledge.isRecipeUnlocked(r.getId()) && 
                    canCraftClient(r)) {
                    hasPrototypes = true;
                    break;
                }
            }
            
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
            
            // 4. Render Prototype Lamp (if any)
            if (hasPrototypes) {
                // Moved left and up to exceed frame slightly
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_RESEARCHABLE_OFF, x - 3, rectY - 3, 0, 0, 16, 16, 16, 16);
            }
            
            // Render tooltip on hover
            if (hovered) {
                guiGraphics.setTooltipForNextFrame(this.font, Component.translatable("category.dontstravemc." + cat.getId()), mouseX, mouseY);
            }
        }
    }

    private void renderRecipeList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        List<TechRecipe> recipes = TechRecipeManager.INSTANCE.getRecipesByCategory(selectedCategory);
        // Grid: 8x8 starting at x18, y25. X-pitch: 18, Y-pitch: 18
        for (int i = 0; i < Math.min(recipes.size(), 64); i++) {
            TechRecipe recipe = recipes.get(i);
            int row = i / 8;
            int col = i % 8;
            int rx = x + 18 + (col * 18);
            int ry = y + 25 + (row * 18);
            
            boolean hovered = mouseX >= rx && mouseX <= rx + 18 && mouseY >= ry && mouseY <= ry + 18;
            
            // Determine state
            KnowledgeComponents knowledge = ModComponents.KNOWLEDGE.get(Minecraft.getInstance().player);
            boolean techMet = knowledge.getTechLevel() >= recipe.getRequiredTechLevel();
            boolean discovered = knowledge.isRecipeUnlocked(recipe.getId());
            boolean matsMet = canCraftClient(recipe);
            
            // Render selection/hover highlight
            if (recipe == selectedRecipe) {
                guiGraphics.fill(rx, ry, rx + 18, ry + 18, 0x66FFFFFF);
            } else if (hovered && techMet) {
                guiGraphics.fill(rx, ry, rx + 18, ry + 18, 0x33FFFFFF);
            }

            // Stacking Logic:
            // 1. If Locked or Uncraftable: render lock_itemframe
            if (!techMet || !matsMet) {
                // Fixed: Removed +1 offset as requested. Drawing at rx, ry with 16x16.
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK_FRAME, rx, ry, 0, 0, 16, 16, 16, 16);
            }
            
            // 2. If Prototype: render bulb (using researchable_off as requested)
            if (techMet && matsMet && !discovered) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_RESEARCHABLE_OFF, rx, ry, 0, 0, 16, 16, 16, 16);
            }

            // 3. If Locked: render lock icon
            if (!techMet) {
                // Fixed: Removed +1 offset
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK, rx, ry, 0, 0, 16, 16, 16, 16);
            }

            // 4. Render Item icon on top
            // Using base rx, ry to align with your background grid preference
            guiGraphics.renderFakeItem(recipe.getResult(), rx, ry);
        }
    }

    private boolean canCraftClient(TechRecipe recipe) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        
        for (Map.Entry<Ingredient, Integer> entry : recipe.getIngredients().entrySet()) {
            int count = 0;
            for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (entry.getKey().test(stack)) {
                    count += stack.getCount();
                }
            }
            if (count < entry.getValue()) return false;
        }
        return true;
    }

    private void renderRecipeDetails(GuiGraphics guiGraphics, int dx, int dy, int mouseX, int mouseY) {
        // dx, dy are the top-left of background_2
        
        // Product Slot at x66, y20 relative to dx (start of whole details panel)
        int px = dx + 66;
        int py = dy + 20;
        guiGraphics.renderFakeItem(selectedRecipe.getResult(), px, py);
        
        // The frame starts at x19 in background_2 atlas and has width 128
        int frameX = dx + 19;
        int labelWidth = 118; // 128 - 10px padding
        int labelPadding = 5;
        
        // 1. Name Section
        int ny = dy + 45;
        guiGraphics.fill(frameX + labelPadding, ny, frameX + labelPadding + labelWidth, ny + 15, 0xB2000000);
        guiGraphics.drawCenteredString(this.font, selectedRecipe.getResult().getHoverName(), frameX + 64, ny + 4, 0xFFFFFFFF);
        
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
        
        int totalWidth = 178 + 20 + 147;
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
                // Check tech level before allowing selection
                KnowledgeComponents knowledge = ModComponents.KNOWLEDGE.get(Minecraft.getInstance().player);
                if (knowledge.getTechLevel() >= recipes.get(i).getRequiredTechLevel()) {
                    selectedRecipe = recipes.get(i);
                    return true;
                }
                return false;
            }
        }

        // Craft Button click
        if (selectedRecipe != null) {
            int frameX = dx + 19;
            int labelWidth = 118;
            int labelPadding = 5;
            int btnX = frameX + labelPadding + (labelWidth - 61) / 2;
            int btnY = dy + 145;
            if (mouseX >= btnX && mouseX <= btnX + 61 && mouseY >= btnY && mouseY <= btnY + 15) {
                // Double check everything on client before sending
                if (canCraftClient(selectedRecipe)) {
                    ClientPlayNetworking.send(new CraftRequestPayload(selectedRecipe.getId()));
                }
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
