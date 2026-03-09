package com.dontstravemc.client;

import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SanityHudOverlay implements HudRenderCallback {

    private static final ResourceLocation SANITY_TEXTURE = ResourceLocation.fromNamespaceAndPath("dontstravemc",
            "textures/gui/sanity.png");

    private float clientSanity = 100.0f;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null)
            return;

        // Get sanity from player
        if (client.player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            clientSanity = manager.getSanity();
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Calculate position: right side of hotbar, above hunger
        // Hotbar center is at screenWidth/2
        // Hunger bar starts at screenWidth/2 + 82 (right side)
        // We want to be 10 pixels above the hunger bar
        int x = screenWidth / 2 + 82;
        int y = screenHeight - 49 - 10; // -49 is hunger bar position, -10 is offset above

        // Draw sanity icons (10 icons, like hunger)
        int maxIcons = 10;
        float sanityPerIcon = 10.0f;

        RenderSystem.enableBlend();

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

        RenderSystem.disableBlend();
    }

    private void drawIcon(GuiGraphics graphics, int x, int y, int u, int v) {
        graphics.blit(SANITY_TEXTURE, x, y, u, v, 9, 9, 27, 9);
    }

    public void setSanity(float sanity) {
        this.clientSanity = sanity;
    }
}
