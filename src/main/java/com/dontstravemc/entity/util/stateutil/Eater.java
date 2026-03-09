package com.dontstravemc.entity.util.stateutil;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public interface Eater {
    SoundEvent getEatingSound();

    // 新增：进食持续的总时间（单位：Tick，20 Tick = 1秒）
    default int getEatingDuration() { return 40; }

    // 新增：咀嚼声播放的频率（每隔多少 Tick 响一次）
    default int getEatingSoundInterval() { return 20; }
    default int getPreEatingDuration() { return 10; }

    void setEating(boolean eating);
    void setEatingItem(ItemStack stack);
}
