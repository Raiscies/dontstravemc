package com.dontstravemc.status;

import com.dontstravemc.status.sanity.SanityComponents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SanityHudOverlay implements HudRenderCallback {
    private static final ResourceLocation SANITY_TEXTURE = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/gui/sanity.png");
    private static final ResourceLocation SANITY_OVERLAY = ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/misc/sanity_overlay.png");
    
    private static float overlayAlpha = 0.0f;
    private static float targetAlpha = 0.0f;
    private static long lastAlphaUpdateTime = -1;
    private static boolean soundPlayed = false;

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.options.hideGui) return;

        // 1. 获取组件
        SanityComponents sanityComp = ModComponents.SANITY.get(client.player);
        float currentSanity = sanityComp.getSanity();
        float rate = sanityComp.getEnvironmentalRate();

        // 2. 屏幕尺寸与基础位置
        int screenWidth = drawContext.guiWidth();
        int screenHeight = drawContext.guiHeight();
        int baseX = screenWidth / 2 + 10;
        int baseY = screenHeight - 48;

        // --- 新增：低理智红视效果 (低于 15% 即 < 30.0f) ---
        long currentTime = net.minecraft.Util.getMillis();
        if (lastAlphaUpdateTime == -1) {
            lastAlphaUpdateTime = currentTime;
        }
        float deltaTime = (currentTime - lastAlphaUpdateTime) / 1000.0f;
        lastAlphaUpdateTime = currentTime;

        if (currentSanity < 30.0f) {
            targetAlpha = 1.0f;
            if (!soundPlayed) {
                client.player.playSound(com.dontstravemc.event.ModSounds.MUSIC_DUSK_GONECRAZIER, 1.0f, 1.0f);
                soundPlayed = true;
            }
        } else if (currentSanity > 35.0f) {
            targetAlpha = 0.0f;
            soundPlayed = false;
        }

        if (overlayAlpha < targetAlpha) {
            overlayAlpha = Math.min(targetAlpha, overlayAlpha + deltaTime / 2.0f); // 2 seconds to fade in
        } else if (overlayAlpha > targetAlpha) {
            overlayAlpha = Math.max(targetAlpha, overlayAlpha - deltaTime / 2.0f); // 2 seconds to fade out
        }

        if (overlayAlpha > 0.0f) {
            renderTendrils(drawContext, screenWidth, screenHeight, overlayAlpha);
        }

        // 3. 渲染 10 个大脑图标
        for (int i = 0; i < 10; i++) {
            float thresholdMin = 180 - (i * 20);
            int uOffset = (currentSanity > thresholdMin + 10) ? 0 : (currentSanity > thresholdMin ? 9 : 18);

            drawContext.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    SANITY_TEXTURE,
                    baseX + (i * 8), baseY,
                    (float)uOffset, 0,
                    9, 9,
                    9, 9,
                    256, 256
            );
        }

        // 4. 在大脑条右侧渲染动态箭头
        // baseX + (10个大脑 * 8像素间隔) + 4像素偏移
        renderArrows(drawContext, rate, baseX + 84, baseY, deltaTracker.getGameTimeDeltaTicks());
    }

    private void renderTendrils(GuiGraphics context, int w, int h, float alpha) {
        // Red overlay processing
        // Use the 13-arg blit overload which supports color tinting (alpha)
        // We use 1x1 texture dimensions to force normalized UVs (0.0 - 1.0)
        
        int color = ARGB.colorFromFloat(alpha, 1.0f, 1.0f, 1.0f);
        
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            SANITY_OVERLAY,
            0, 0,              // x, y
            0.0f, 0.0f,        // u, v
            w, h,              // width, height
            1, 1,              // texture snippet width, height (mapped to 1.0)
            1, 1,              // texture total width, height (mapped to 1.0)
            color              // tint color with alpha
        );
    }

    private void renderArrows(GuiGraphics drawContext, float rate, int x, int y, float partialTicks) {
        float absRate = Math.abs(rate);
        boolean isRising = rate > 0;

        // 1. 判定档位
        int size = 0;
        if (isRising) {
            if (absRate > 0.2f) size = 3; else if (absRate > 0.1f) size = 2; else if (absRate > 0.01f) size = 1;
        } else {
            if (absRate > 0.3f) size = 3; else if (absRate > 0.1f) size = 2; else if (absRate > 0.02f) size = 1;
        }
        if (size == 0) return;

        // 2. UV 坐标
        int u = (size == 1) ? (isRising ? 29 : 28) : (size == 2 ? 35 : 43);
        int v = isRising ? (size == 3 ? 10 : 11) : (size == 1 ? 2 : (size == 2 ? 1 : 0));
        int w = (size == 1) ? (isRising ? 4 : 5) : (size == 2 ? 7 : 9);
        int h = (size == 1) ? 6 : (size == 2 ? 8 : 10);


        float clientTicks = Minecraft.getInstance().level.getGameTime() + partialTicks;
        float speed = 0.6f; // 调节速度，数值越大越快
        int spacing = 20;   // 箭头间距


        float scrollOffset = (clientTicks * speed) % spacing;


        drawContext.enableScissor(x, y, x + 12, y + 10);

        // 5. 渲染循环
        for (int i = -1; i <= 1; i++) {
            float drawY;
            if (isRising) {
                // 上升：从底部出现往上走
                drawY = y + (spacing - scrollOffset) + (i * spacing);
            } else {
                // 下降：从顶部出现往下走
                drawY = y + (scrollOffset - spacing) + (i * spacing);
            }

            drawContext.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    SANITY_TEXTURE,
                    x, (int)drawY,
                    (float)u, (float)v,
                    w, h,
                    256, 256
            );
        }

        drawContext.disableScissor();
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new SanityHudOverlay());
    }
}