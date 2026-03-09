package com.dontstravemc.status.sanity;

import com.dontstravemc.status.ModComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;


public class PlayerSanityComponent implements SanityComponents, AutoSyncedComponent, ServerTickingComponent {
    private float sanity = 200.0f;
    private float environmentalRate = 0.0f; // 每秒的变化量
    private final Player player;

    public PlayerSanityComponent(Player player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        // 每 5 tick 同步一次即可，不需要每 tick 同步，节省性能
        if (player.level().getGameTime() % 5 == 0) {
            ModComponents.SANITY.sync(this.player);
        }
    }

    @Override public float getSanity() { return this.sanity; }
    @Override public float getEnvironmentalRate() { return this.environmentalRate; }

    @Override
    public void setSanity(float value) {
        this.sanity = Mth.clamp(value, 0.0f, 200.0f);
    }

    @Override
    public void addSanity(float value) {
        // 瞬时修改：不影响 environmentalRate，直接改数值
        setSanity(this.sanity + value);
    }

    @Override
    public void setEnvironmentalRate(float rate) {
        this.environmentalRate = rate;
    }

    @Override
    public void applyContinuousChange() {
        // 将每秒变化率应用到每 tick (1/20)
        if (this.environmentalRate != 0) {
            addSanity(this.environmentalRate / 20.0f);
        }
    }

    // --- CCA 持久化 ---
    @Override
    public void readData(ValueInput input) {
        if (input instanceof net.minecraft.world.level.storage.TagValueInput tagInput) {
            this.sanity = tagInput.getFloatOr("sanity_value", 200.0f);
        }
    }

    @Override
    public void writeData(ValueOutput output) {
        if (output instanceof net.minecraft.world.level.storage.TagValueOutput tagOutput) {
            tagOutput.putFloat("sanity_value", this.sanity);
        }
    }

    // --- 网络同步 ---
    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeFloat(this.sanity);
        buf.writeFloat(this.environmentalRate);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.sanity = buf.readFloat();
        this.environmentalRate = buf.readFloat();
    }
}