package com.dontstravemc.mixin.client;

import com.dontstravemc.shader.DynamicSanityUniform;
import com.dontstravemc.status.SanityShaderData;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.UniformValue;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(PostPass.class)
public class MixinPostPass {

    @Shadow @Final private Map<String, GpuBuffer> customUniforms;

    // 1. 结构注入 (保持不变)
    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static Map<String, List<UniformValue>> injectSanityUniforms(Map<String, List<UniformValue>> original) {
        if (original.containsKey("SanityConfig")) {
            Map<String, List<UniformValue>> mutableMap = new HashMap<>(original);
            List<UniformValue> dynamicList = new ArrayList<>();

            dynamicList.add(new DynamicSanityUniform("SanityLevel"));
            dynamicList.add(new DynamicSanityUniform("GameTime"));
            dynamicList.add(new DynamicSanityUniform("Padding"));
            dynamicList.add(new DynamicSanityUniform("Padding"));

            mutableMap.put("SanityConfig", dynamicList);
            return mutableMap;
        }
        return original;
    }

    // 2. 【新增】构造后替换 Buffer (核心修复)
    // 原版构造函数创建的 Buffer 没有 COPY_DST 权限，我们得把它换掉
    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceBufferWithDynamicOne(RenderPipeline renderPipeline, ResourceLocation resourceLocation, Map<String, List<UniformValue>> map, List<PostPass.Input> list, CallbackInfo ci) {
        if (this.customUniforms.containsKey("SanityConfig")) {
            // 1. 获取并关闭旧的(权限不足的) Buffer，防止内存泄漏
            GpuBuffer oldBuffer = this.customUniforms.get("SanityConfig");
            oldBuffer.close();

            // 2. 计算需要的权限标志
            // USAGE_UNIFORM (128) | USAGE_COPY_DST (1)
            // 这样既能当 Uniform 用，也能被 CPU 写入
            int flags = GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST;

            // 3. 创建新 Buffer
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // 创建 16 字节的初始零数据
                ByteBuffer initialData = stack.calloc(16);

                GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "SanityConfig_Dynamic", // 名字随便起，方便调试
                        flags,
                        initialData
                );

                // 4. 替换进 Map
                this.customUniforms.put("SanityConfig", newBuffer);
            }
        }
    }

    // 3. 动态更新 (保持 writeToBuffer 逻辑)
    @Inject(method = "addToFrame", at = @At("HEAD"))
    private void updateSanityBufferPerFrame(
            FrameGraphBuilder frameGraphBuilder,
            Map<ResourceLocation, ResourceHandle<RenderTarget>> map,
            com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
            CallbackInfo ci
    ) {
        GpuBuffer buffer = this.customUniforms.get("SanityConfig");

        if (buffer != null) {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                Std140Builder builder = Std140Builder.onStack(stack, 16);
                builder.putFloat(SanityShaderData.currentSanity);
                builder.putFloat(SanityShaderData.gameTime);
                builder.putFloat(0.0f);
                builder.putFloat(0.0f);

                // 现在 buffer 拥有了 COPY_DST 权限，这句话就不会报错了
                GpuBufferSlice targetSlice = new GpuBufferSlice(buffer, 0, 16);
                encoder.writeToBuffer(targetSlice, builder.get());
            }
        }
    }
}