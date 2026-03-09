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
        renderCategories(guiGraphics, x - 25, y - 20, mouseX, mouseY);

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
            // Unselected: x3, y48, 20x20
            // Hovered: x3, y72, 20x20
            // Selected: x2, y96, 22x22
            int frameU = 3;
            int frameV = 48; // Default
            int fW = 20;
            int fH = 20;
            
            if (cat == selectedCategory) {
                frameU = 2;
                frameV = 96; // Selected
                fW = 22;
                fH = 22;
            } else if (hovered) {
                frameU = 3;
                frameV = 72; // Hovered
                fW = 20;
                fH = 20;
            }
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CATEGORY_BG_ATLAS, x, rectY, frameU, frameV, fW, fH, 256, 256);

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
            
            // Stacking Logic: (Layered)
            
            // 1. If Locked or Uncraftable: render lock_itemframe (16x16 centered)
            if ((!techMet && !discovered) || !matsMet) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK_FRAME, rx, ry, 0, 0, 16, 16, 16, 16);
            }

            // 2. Render Item icon (16x16)
            guiGraphics.renderFakeItem(recipe.getResult(), rx, ry);

            // 3. If Locked: render lock icon on top (16x16 centered)
            if (!techMet && !discovered) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_LOCK, rx, ry, 0, 0, 16, 16, 16, 16);
            }

            // 4. If Prototype: render bulb on top (16x16 centered)
            if (techMet && matsMet && !discovered) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_RESEARCHABLE_OFF, rx, ry, 0, 0, 16, 16, 16, 16);
            }

            // 5. White Selection/Hover Highlight - Highest layer
            if (recipe == selectedRecipe) {
                guiGraphics.fill(rx - 1, ry - 1, rx + 17, ry + 17, 0x66FFFFFF);
            } else if (hovered && (techMet || discovered)) {
                guiGraphics.fill(rx - 1, ry - 1, rx + 17, ry + 17, 0x33FFFFFF);
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
        
        // Final Alignment: Center everything under the product icon (at 66 + 16/2 = 74)
        int contentCenterX = dx + 74; 
        int maxLabelWidth = 118;
        
        // 2. Name Section (Dynamic Width & Centered under product, Wrapped every 7 chars)
        String nameStr = selectedRecipe.getResult().getHoverName().getString();
        String wrappedName = wrapTextByChars(nameStr, 7);
        List<FormattedCharSequence> nameLines = this.font.split(Component.literal(wrappedName), maxLabelWidth - 10);
        
        int maxNameWidth = 0;
        for (FormattedCharSequence line : nameLines) maxNameWidth = Math.max(maxNameWidth, this.font.width(line));
        
        int namePadding = 8;
        int nameBarWidth = Math.min(maxNameWidth + namePadding * 2, maxLabelWidth);
        int ny = dy + 45;
        int nameBarHeight = nameLines.size() * 10 + 5;
        guiGraphics.fill(contentCenterX - nameBarWidth / 2, ny, contentCenterX + nameBarWidth / 2, ny + nameBarHeight, 0xB2000000);
        
        for (int i = 0; i < nameLines.size(); i++) {
            int lineWidth = this.font.width(nameLines.get(i));
            guiGraphics.drawString(this.font, nameLines.get(i), contentCenterX - lineWidth / 2, ny + 4 + (i * 10), 0xFFFFFFFF, false);
        }
        
        // 3. Ingredients Section (Dynamic Width & Centered under product)
        int iy = ny + nameBarHeight + 5; // Re-align below name
        int totalIngWidth = selectedRecipe.getIngredients().size() * 20 - 4; 
        int ingBarPadding = 8;
        int ingBarWidth = Math.min(totalIngWidth + ingBarPadding * 2, maxLabelWidth);
        guiGraphics.fill(contentCenterX - ingBarWidth / 2, iy, contentCenterX + ingBarWidth / 2, iy + 25, 0xB2000000);
        
        int j = 0;
        int startIngX = contentCenterX - totalIngWidth / 2;
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

        // 4. Description Section (Dynamic Width & Centered, Wrapped every 7 chars)
        int descY = iy + 30; // Re-align below ingredients
        String descStr = Component.translatable(selectedRecipe.getDescriptionKey()).getString();
        String wrappedDesc = wrapTextByChars(descStr, 7);
        List<FormattedCharSequence> descLines = this.font.split(Component.literal(wrappedDesc), maxLabelWidth - 10);
        
        int maxDescLineWidth = 0;
        for (FormattedCharSequence line : descLines) maxDescLineWidth = Math.max(maxDescLineWidth, this.font.width(line));
        
        int descPadding = 10;
        int descBarWidth = Math.min(maxDescLineWidth + descPadding * 2, maxLabelWidth);
        int descBarHeight = descLines.size() * 10 + 10;
        
        guiGraphics.fill(contentCenterX - descBarWidth / 2, descY, contentCenterX + descBarWidth / 2, descY + descBarHeight, 0xB2000000);
        for (int i = 0; i < Math.min(descLines.size(), 6); i++) {
            int lineWidth = this.font.width(descLines.get(i));
            guiGraphics.drawString(this.font, descLines.get(i), contentCenterX - lineWidth / 2, descY + 5 + (i * 10), 0xFFCCCCCC, false);
        }

        // 5. Craft Button (Centered under everything)
        int btnWidth = 61;
        int btnX = contentCenterX - btnWidth / 2;
        int btnY = dy + 155; 
        boolean hoveredBtn = mouseX >= btnX && mouseX <= btnX + 61 && mouseY >= btnY && mouseY <= btnY + 15;
        guiGraphics.fill(btnX, btnY, btnX + 61, btnY + 15, hoveredBtn ? 0xB200AA00 : 0xB2008800);
        guiGraphics.drawCenteredString(this.font, "CRAFT", contentCenterX, btnY + 4, 0xFFFFFFFF);
    }

    private String wrapTextByChars(String text, int wrapAt) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if ((i + 1) % wrapAt == 0 && i != text.length() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
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
            int rectY = y - 20 + (i * 22);
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
