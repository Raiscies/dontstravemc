package com.dontstravemc.status;

import com.dontstravemc.mixin.client.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class SanityVisualHandler {
    // 修正：确保这里的 ID 与你的 JSON 资源路径严格一致
    // 对应 assets/dontstravemc/post_effect/sanity_distort.json
    private static final ResourceLocation DISTORT = ResourceLocation.fromNamespaceAndPath("dontstravemc", "sanity_distort");

    private static float smoothedSanity = 1.0f;

    public static void update() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameRenderer == null) return;

        // 1. 获取理智百分比
        float realPercent = ModComponents.SANITY.maybeGet(client.player)
                .map(sanity -> sanity.getSanity() / 200.0f)
                .orElse(1.0f);

        // 2. 平滑插值
        smoothedSanity += (realPercent - smoothedSanity) * 0.05f;

        // 3. 更新全局数据供 MixinPostPass 读取
        SanityShaderData.currentSanity = smoothedSanity;
        SanityShaderData.gameTime = (float) (System.currentTimeMillis() % 1000000) / 1000.0f;

        GameRendererAccessor accessor = (GameRendererAccessor) client.gameRenderer;

        // 4. 控制逻辑优化
        // 为了防止临界点反复开启关闭导致的微小卡顿，建议只要低于 0.99 且滤镜还没加载，就加载它
        if (smoothedSanity < 0.99f) {
            // 如果当前没有加载任何滤镜，或者滤镜不是我们要的，就设置它
            if (accessor.getPostEffectId() == null || !accessor.getPostEffectId().equals(DISTORT)) {
                accessor.setPostEffectId(DISTORT);
                // 1.21.10 还需要确保 checkPostEffect 被触发，通常 setPostEffectId 足够
            }
            accessor.setEffectActive(true);
        } else {
            // 回满后彻底卸载
            if (accessor.getPostEffectId() != null && accessor.getPostEffectId().equals(DISTORT)) {
                accessor.setPostEffectId(null);
                accessor.setEffectActive(false);
            }
        }
    }
}