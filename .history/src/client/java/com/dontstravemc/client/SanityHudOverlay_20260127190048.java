package com.dontstravemc.client;

import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SanityHudOverlay implements HudRenderCallback {

    private static final ResourceLocation SANITY_TEXTURE = ResourceLocation.fromNamespaceAndPath("dontstravemc",
            "textures/gui/sanity.png");

    private float clientSanity = 100.0f;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || (client.gameMode != null && !client.gameMode.canHurtPlayer()))
            return;

        // Get sanity from player
        if (client.player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            clientSanity = manager.getSanity();
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Calculate position: right side of hotbar, above hunger
        // Standard hunger bar is at right side.
        // We want to be above it.
        int x = screenWidth / 2 + 82;
        int y = screenHeight - 39 - 10; // moved down slightly to be just above hunger

        // Enable blend for transparency
        // Use reflection to avoid compilation errors if mapping is different or IDE is
        // confused
        try {
            // Check if RenderSystem methods are accessible directly first (optimization)
            // But since we had issues, strict reflection is safer for now
            Class<?> renderSystemClass = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            java.lang.reflect.Method enableBlend = renderSystemClass.getMethod("enableBlend");
            enableBlend.invoke(null);

            java.lang.reflect.Method defaultBlendFunc = renderSystemClass.getMethod("defaultBlendFunc");
            defaultBlendFunc.invoke(null);
        } catch (Exception e) {
            // If reflection fails, we might be on a version where these don't exist
            // Try fallback or just ignore
        }

        // Draw sanity icons (10 icons, like hunger)
        int maxIcons = 10;
        float sanityPerIcon = 10.0f;

        for (int i = 0; i < maxIcons; i++) {
            int iconX = x - (i * 8) - 9; // Draw from right to left
            int iconY = y;

            // Calculate how full this icon should be
            float currentIconSanity = clientSanity - (i * sanityPerIcon);

            if (currentIconSanity >= sanityPerIcon) {
                // Full icon
                drawIcon(guiGraphics, iconX, iconY, 0, 0);
            } else if (currentIconSanity > 0) {
                // Partial icon
                if (currentIconSanity >= sanityPerIcon / 2) {
                    drawIcon(guiGraphics, iconX, iconY, 9, 0); // Half icon
                } else {
                    // Empty but show outline
                    drawIcon(guiGraphics, iconX, iconY, 18, 0);
                }
            } else {
                // Completely empty
                drawIcon(guiGraphics, iconX, iconY, 18, 0);
            }
        }

        try {
            Class<?> renderSystemClass = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            java.lang.reflect.Method disableBlend = renderSystemClass.getMethod("disableBlend");
            disableBlend.invoke(null);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void drawIcon(GuiGraphics graphics, int x, int y, int u, int v) {
        graphics.blit(SANITY_TEXTURE, x, y, u, v, 9, 9, 27, 9);
    }

    public void setSanity(float sanity) {
        this.clientSanity = sanity;
    }
}
