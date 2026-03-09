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
            
            // Render selection/hover highlight (18x18)
            if (recipe == selectedRecipe) {
                // Adjusted: Shifted -1, -1 to fix the user-reported offset bug
                guiGraphics.fill(rx - 1, ry - 1, rx + 17, ry + 17, 0x66FFFFFF);
            } else if (hovered && (techMet || discovered)) {
                guiGraphics.fill(rx - 1, ry - 1, rx + 17, ry + 17, 0x33FFFFFF);
            }

            // Stacking Logic:
            // 1. If Locked or Uncraftable: render lock_itemframe (18x18)
            if ((!techMet && !discovered) || !matsMet) {
                // Adjusted: Shifted to rx-1, ry-1 to align with selection/grid origin
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK_FRAME, rx - 1, ry - 1, 0, 0, 18, 18, 18, 18);
            }

            // 2. Render Item icon (16x16)
            // Adjusted: Shifted to rx-1, ry-1 as requested to fix 1px offset
            guiGraphics.renderFakeItem(recipe.getResult(), rx - 1, ry - 1);

            // 3. If Locked: render lock icon on top
            if (!techMet && !discovered) {
                // Adjusted: Shifted to rx-1, ry-1
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK, rx - 1, ry - 1, 0, 0, 18, 18, 18, 18);
            }

            // 4. If Prototype: render bulb on top of everything
            if (techMet && matsMet && !discovered) {
                // Adjusted: Shifted to rx-1, ry-1
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_RESEARCHABLE_OFF, rx - 1, ry - 1, 0, 0, 18, 18, 18, 18);
            }
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
        
        // 1. Product Slot
        int px = dx + 66;
        int py = dy + 20;
        guiGraphics.renderFakeItem(selectedRecipe.getResult(), px, py);
        
        int frameCenterX = dx + 19 + 64; // Center of the info area (19 arrow + 128/2)
        int maxLabelWidth = 118;
        
        // 2. Name Section (Dynamic Width & Centered)
        Component name = selectedRecipe.getResult().getHoverName();
        int nameWidth = this.font.width(name);
        int namePadding = 10;
        int nameBarWidth = Math.min(nameWidth + namePadding * 2, maxLabelWidth);
        int ny = dy + 45;
        guiGraphics.fill(frameCenterX - nameBarWidth / 2, ny, frameCenterX + nameBarWidth / 2, ny + 15, 0xB2000000);
        guiGraphics.drawCenteredString(this.font, name, frameCenterX, ny + 4, 0xFFFFFFFF);
        
        // 3. Ingredients Section (Dynamic Width & Centered)
        int iy = dy + 65;
        int totalIngWidth = selectedRecipe.getIngredients().size() * 20 - 4; // 16px item + 4px relative spacing
        int ingBarPadding = 10;
        int ingBarWidth = Math.min(totalIngWidth + ingBarPadding * 2, maxLabelWidth);
        guiGraphics.fill(frameCenterX - ingBarWidth / 2, iy, frameCenterX + ingBarWidth / 2, iy + 25, 0xB2000000);
        
        int j = 0;
        int startIngX = frameCenterX - totalIngWidth / 2;
        for (Map.Entry<Ingredient, Integer> entry : selectedRecipe.getIngredients().entrySet()) {
            ItemStack[] stacks = entry.getKey().items().map(h -> new ItemStack(h.value())).toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                int itemX = startIngX + (j * 20);
                int itemY = iy + 4;
                ItemStack displayStack = stacks[0].copy();
                int requiredCount = entry.getValue();
                displayStack.setCount(requiredCount);
                
                int heldCount = getHeldCount(entry.getKey());
                int color = heldCount >= requiredCount ? 0xFFFFFFFF : 0xFFFF0000;
                
                guiGraphics.renderItem(displayStack, itemX, itemY);
                String countText = String.valueOf(requiredCount);
                guiGraphics.drawString(this.font, countText, itemX + 17 - this.font.width(countText), itemY + 9, color, true);
                j++;
            }
        }

        // 4. Description Section (Dynamic Width & Centered)
        int descY = dy + 95;
        List<FormattedCharSequence> descLines = this.font.split(Component.translatable(selectedRecipe.getDescriptionKey()), maxLabelWidth - 10);
        int maxLineWidth = 0;
        for (FormattedCharSequence line : descLines) {
            maxLineWidth = Math.max(maxLineWidth, this.font.width(line));
        }
        int descPadding = 10;
        int descBarWidth = Math.min(maxLineWidth + descPadding * 2, maxLabelWidth);
        int descBarHeight = Math.min(descLines.size() * 10 + 10, 50);
        
        guiGraphics.fill(frameCenterX - descBarWidth / 2, descY, frameCenterX + descBarWidth / 2, descY + descBarHeight, 0xB2000000);
        for (int i = 0; i < Math.min(descLines.size(), 4); i++) {
            int lineWidth = this.font.width(descLines.get(i));
            guiGraphics.drawString(this.font, descLines.get(i), frameCenterX - lineWidth / 2, descY + 5 + (i * 10), 0xFFCCCCCC, false);
        }

        // 5. Craft Button
        int btnWidth = 61;
        int btnX = frameCenterX - btnWidth / 2;
        int btnY = dy + 150; // Centered at bottom
        boolean hoveredBtn = mouseX >= btnX && mouseX <= btnX + 61 && mouseY >= btnY && mouseY <= btnY + 15;
        guiGraphics.fill(btnX, btnY, btnX + 61, btnY + 15, hoveredBtn ? 0xB200AA00 : 0xB2008800);
        guiGraphics.drawCenteredString(this.font, "CRAFT", frameCenterX, btnY + 4, 0xFFFFFFFF);
    }

    private int getHeldCount(Ingredient ingredient) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
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
                // Allow selection if tech is met OR if it was already discovered
                KnowledgeComponents knowledge = ModComponents.KNOWLEDGE.get(Minecraft.getInstance().player);
                TechRecipe selected = recipes.get(i);
                if (knowledge.getTechLevel() >= selected.getRequiredTechLevel() || 
                    knowledge.isRecipeUnlocked(selected.getId())) {
                    selectedRecipe = selected;
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
