package com.dontstravemc.render.state;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.Map;

public class SpiderDenRenderState extends LivingEntityRenderState implements GeoRenderState {

    // 1. 定义数据存储容器 (这是 GeoRenderState 接口要求的核心)
    private final Map<DataTicket<?>, Object> dataMap = new Reference2ObjectOpenHashMap<>();

    // 2. 如果你需要存储坐标用于渲染定位（上一轮对话提到的）
    public double x;
    public double y;
    public double z;

    public boolean isDead;
    // 如果有自定义状态（比如等级），定义在这里
    // public int tier;

    // --- 实现 GeoRenderState 接口要求的 4 个方法 ---

    @Override
    public <D> void addGeckolibData(DataTicket<D> dataTicket, @Nullable D data) {
        this.dataMap.put(dataTicket, data);
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.dataMap.containsKey(dataTicket);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        return (D) this.dataMap.get(dataTicket);
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.dataMap;
    }
}