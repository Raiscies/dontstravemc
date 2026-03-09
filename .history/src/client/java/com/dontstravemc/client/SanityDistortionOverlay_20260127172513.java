package com.dontstravemc.client;

import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SanityDistortionOverlay {

    private static final float DISTORTION_THRESHOLD = 30.0f;
    private static float clientSanity = 100.0f;

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null)
            return;

        // Get sanity from player
        if (client.player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            clientSanity = manager.getSanity();
        }

        // Only apply distortion if sanity is low
        if (clientSanity >= DISTORTION_THRESHOLD)
            return;

        // Calculate distortion intensity (0.0 to 1.0)
        // At 30 sanity: 0.0, at 0 sanity: 1.0
        float intensity = 1.0f - (clientSanity / DISTORTION_THRESHOLD);
        intensity = Math.min(1.0f, Math.max(0.0f, intensity)); // Clamp to [0, 1]

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Apply vignette effect - darker edges
        int vignetteAlpha = (int) (intensity * 180); // Max 180/255 opacity
        int vignetteColor = (vignetteAlpha << 24) | 0x000000; // Black with calculated alpha

        // Draw vignette overlay
        guiGraphics.fill(0, 0, screenWidth, screenHeight, vignetteColor);

        // Add occasional "pulse" effect based on game time
        long gameTime = client.level != null ? client.level.getGameTime() : 0;
        float pulse = (float) Math.sin(gameTime * 0.05f) * 0.5f + 0.5f; // 0.0 to 1.0

        if (intensity > 0.5f && pulse > 0.8f) {
            // Flash dark overlay for very low sanity
            int flashAlpha = (int) ((intensity - 0.5f) * 2.0f * 100);
            int flashColor = (flashAlpha << 24) | 0x000000;
            guiGraphics.fill(0, 0, screenWidth, screenHeight, flashColor);
        }
    }

    public static void setSanity(float sanity) {
        clientSanity = sanity;
    }
}
