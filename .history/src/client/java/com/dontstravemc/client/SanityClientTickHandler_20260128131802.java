package com.dontstravemc.client;

import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
                try {
                    // Use reflection to find loadEffect method by checking signature or name
                    // In Yarn/Mojang it's usually loadEffect
                    java.lang.reflect.Method loadEffect = null;
                    try {
                        loadEffect = client.gameRenderer.getClass().getMethod("loadEffect", ResourceLocation.class);
                    } catch (NoSuchMethodException e) {
                        // Try searching by parameter type if name is obfuscated
                        for (java.lang.reflect.Method m : client.gameRenderer.getClass().getMethods()) {
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(ResourceLocation.class)
                                    && m.getReturnType().equals(void.class)) {
                                loadEffect = m;
                                break;
                            }
                        }
                    }

                    if (loadEffect != null) {
                        loadEffect.invoke(client.gameRenderer, SHADER_LOC);
                        shaderActive = true;
                        // Debug message
                        client.player.displayClientMessage(Component.literal("Sanity Low! Distortion Enabled."), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (shaderActive) {
                // Unload shader if active
                try {
                    java.lang.reflect.Method shutdownEffect = null;
                    try {
                        shutdownEffect = client.gameRenderer.getClass().getMethod("shutdownEffect");
                    } catch (NoSuchMethodException e) {
                        // naming might vary
                        for (java.lang.reflect.Method m : client.gameRenderer.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && m.getName().equals("shutdownEffect")) {
                                shutdownEffect = m;
                                break;
                            }
                        }
                    }

                    if (shutdownEffect != null) {
                        shutdownEffect.invoke(client.gameRenderer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                shaderActive = false;
                // Debug message
                client.player.displayClientMessage(Component.literal("Sanity Restored. Distortion Disabled."), true);
            }
        }
    }
}
