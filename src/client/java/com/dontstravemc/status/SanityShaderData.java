package com.dontstravemc.status;

public class SanityShaderData {
    public static float currentSanity = 1.0f;
    public static float gameTime = 0.0f;

    // 1.21.10 常用逻辑：每帧在客户端更新这些值
    public static void update(float sanity, float time) {
        currentSanity = sanity;
        gameTime = time;
    }
}
