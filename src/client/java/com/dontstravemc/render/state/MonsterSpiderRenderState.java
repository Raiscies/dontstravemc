package com.dontstravemc.render.state;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MonsterSpiderRenderState extends LivingEntityRenderState implements GeoRenderState {
    // 你的自定义状态位
    public boolean isAsleep;
    public boolean isAlert;
    public boolean isAggressive;
    public boolean isEating;
    public boolean isHurt;
    public boolean isDead;


    // --- 修复报错：满足 GeoRenderState 接口的所有要求 ---
    private final Map<DataTicket<?>, Object> dataMap = new Reference2ObjectOpenHashMap<>();

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