package com.dontstravemc.shader;

import com.dontstravemc.status.SanityShaderData;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.renderer.UniformValue;

public record DynamicSanityUniform(String fieldName) implements UniformValue {

    @Override
    public void writeTo(Std140Builder builder) {
        // 必须覆盖所有可能的 fieldName，否则 buffer 会缺数据
        if ("SanityLevel".equals(fieldName)) {
            builder.putFloat(SanityShaderData.currentSanity);
        } else if ("GameTime".equals(fieldName)) {
            builder.putFloat(SanityShaderData.gameTime);
        } else {
            // 处理 Padding 或未知字段，写入 0.0 以保持对齐
            builder.putFloat(0.0f);
        }
    }

    @Override
    public void addSize(Std140SizeCalculator calculator) {
        calculator.putFloat(); // 4 bytes
    }

    @Override
    public Type type() {
        return Type.FLOAT;
    }
}