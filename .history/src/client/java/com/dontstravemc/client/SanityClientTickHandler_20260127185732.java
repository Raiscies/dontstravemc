package com.dontstravemc.client;

import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class SanityClientTickHandler implements ClientTickEvents.EndTick {

    // Using the vanilla wobble shader
    private static final ResourceLocation SHADER_LOC = ResourceLocation
            .withDefaultNamespace("shaders/post/wobble.json");
    private boolean shaderActive = false;

    @Override
    public void onEndTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        // Check if game is paused
        if (client.isPaused()) {
            return;
        }

        float sanity = 100.0f;
        if (client.player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            sanity = manager.getSanity();
        }

        // Threshold for distortion (e.g., below 30 sanity)
        if (sanity < 30.0f) {
            if (!shaderActive) {
                // Load shader if not already active
                if (client.gameRenderer.currentEffect() == null) {
                    client.gameRenderer.loadEffect(SHADER_LOC);
                    shaderActive = true;
                }
            }
        } else {
            if (shaderActive) {
                // Unload shader if active
                client.gameRenderer.shutdownEffect();
                shaderActive = false;
            }
        }
    }
}
