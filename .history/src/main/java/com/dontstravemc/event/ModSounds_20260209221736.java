package com.dontstravemc.event; // 确保包名和你项目一致

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final String MOD_ID = "dontstravemc";
    // 1. 定义 ResourceLocation (必须对应 sounds.json 里的 key)
    public static final SoundEvent MONSTER_SPIDER_ATTACK = register("monster_spider_attack");
    public static final SoundEvent MONSTER_SPIDER_STEP = register("monster_spider_step");
    public static final SoundEvent MONSTER_SPIDER_DEATH = register("monster_spider_death");
    public static final SoundEvent MONSTER_SPIDER_SCREAM = register("monster_spider_scream");
    public static final SoundEvent MONSTER_SPIDER_EAT = register("monster_spider_eat");
    public static final SoundEvent MONSTER_SPIDER_SLEEPING = register("monster_spider_sleeping");
    public static final SoundEvent MONSTER_SPIDER_JUMP = register("monster_spider_jump");
    public static final SoundEvent MONSTER_SPIDER_WAKEUP = register("monster_spider_wakeup");
    public static final SoundEvent MONSTER_SPIDER_HURT = register("monster_spider_hurt");
    public static final SoundEvent MONSTER_SPIDER_SLEEP = register("monster_spider_sleep");
    public static final SoundEvent SPIDERDEN_HURT = register("spiderden_hurt");
    public static final SoundEvent SPIDERDEN_DEATH = register("spiderden_death");
    public static final SoundEvent MUSIC_DUSK_GONECRAZIER = register("music_dusk_gonecrazier");
    public static final SoundEvent SANITY_LOOP1 = register("sanity_loop1");
    public static final SoundEvent SANITY_LOOP2 = register("sanity_loop2");
    public static final SoundEvent SANITY_RANDOM = register("sanity_random");
    public static final SoundEvent CRAZY_EXTRA = register("crazy_extra");

    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
        // 使用 createVariableRangeEvent 创建可以改变距离范围的声音事件
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    // 在模组初始化类中调用此方法
    public static void registerSounds() {
        // 此方法通过静态字段的加载完成自动注册
    }
}