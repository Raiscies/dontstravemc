package com.dontstravemc.status.sanity;

import net.minecraft.server.level.ServerPlayer; // 必须是 ServerPlayer
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public interface SanityComponents extends Component, AutoSyncedComponent {
    float getSanity();
    void setSanity(float value);
    void addSanity(float value); // 用于瞬时修改（食物、花）

    // 核心改进：环境变化率
    float getEnvironmentalRate();
    void setEnvironmentalRate(float rate);

    // 应用每刻的变化（rate / 20）
    void applyContinuousChange();

    @Override void readData(ValueInput input);
    @Override void writeData(ValueOutput output);
}