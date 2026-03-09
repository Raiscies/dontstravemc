package com.dontstravemc.status.sanity;

import com.dontstravemc.status.ModComponents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

public class SanityLogicHandler {

    public static void register() {
        // 监听服务器 Tick 结束事件
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tickCount = server.getTickCount();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SanityComponents sanityComp = ModComponents.SANITY.get(player);

                // --- 1. 每秒 (20 ticks) 重新计算一次环境变化率 ---
                // 这保证了性能，且变化率只与环境（光、怪、装）有关
                if (tickCount % 20 == 0) {
                    float totalRate = 0.0f;
                    totalRate += getLightModifier(player);      // 光照影响
                    totalRate += getAuraModifier(player);       // 怪物光环影响
                    totalRate += getEquipmentModifier(player);  // 装备影响

                    // 将计算出的速率存入组件，UI 上的箭头将根据这个值显示
                    sanityComp.setEnvironmentalRate(totalRate);
                }

                // --- 2. 每一 Tick 都执行微量增减 ---
                // 这个方法内部会将 environmentalRate / 20.0f 加到当前 sanity 上
                // 实现了数值的丝滑流动，而不是每秒跳变
                sanityComp.applyContinuousChange();

                // --- 3. 实时同步到客户端 ---
                // 解决“重进世界才生效”的关键。每 5 tick 同步一次，
                // 配合客户端 SanityVisualHandler 的 smoothedSanity 插值，视觉效果极佳。
                if (tickCount % 5 == 0) {
                    ModComponents.SANITY.sync(player);
                }
            }
        });
    }

    private static float getLightModifier(ServerPlayer player) {
        // 获取玩家当前位置的原始亮度 (0-15)
        int light = player.level().getLightEngine().getRawBrightness(player.blockPosition(), 0);

        if (light >= 12) return 0.2f;    // 明亮处：每秒恢复 0.2
        if (light >= 7)  return -0.1f;   // 昏暗处：每秒掉 0.1
        return -1.2f;                    // 黑暗处：每秒掉 1.2 (饥荒风格)
    }

    private static float getAuraModifier(ServerPlayer player) {
        float auraDelta = 0.0f;
        // 扫描附近 8 格内的怪物
        List<Monster> monsters = player.level().getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(8.0),
                m -> m.isAlive()
        );

        for (Monster m : monsters) {
            double dist = m.distanceTo(player);
            // 距离越近掉得越多：分母加1防止除以0
            auraDelta -= (float) (0.6 / (dist + 1.0));
        }
        return auraDelta;
    }

    private static float getEquipmentModifier(ServerPlayer player) {
        float dapper = 0.0f;
        // 检查盔甲位
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                // 示例：金头盔提供回 san 效果
                if (stack.getItem() == Items.GOLDEN_HELMET) {
                    dapper += 0.3f;
                }
                // 示例：下界合金盔甲比较沉重，略微降 san
                if (stack.getItem() == Items.NETHERITE_CHESTPLATE) {
                    dapper -= 0.1f;
                }
            }
        }
        return dapper;
    }
}