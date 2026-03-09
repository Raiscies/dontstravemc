package com.dontstravemc.entity.component;

import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class DenSpawnerComponent {
    private int maxOccupants;        // 最大容量（总数）
    private int currentInside;       // 当前在巢穴里的数量（库存）
    private final List<UUID> activeOccupants = new ArrayList<>(); // 追踪在外活动的生物UUID

    private int regenCooldownTicks;
    private int remainingRegenTicks;
    private final Supplier<EntityType<? extends Mob>> entitySupplier;

    public DenSpawnerComponent(Supplier<EntityType<? extends Mob>> entitySupplier, int max, int cooldownSeconds) {
        this.entitySupplier = entitySupplier;
        this.maxOccupants = max;
        this.currentInside = max; // 初始状态，巢穴是满的
        this.regenCooldownTicks = cooldownSeconds * 20;
        this.remainingRegenTicks = this.regenCooldownTicks;
    }

    /**
     * 每Tick运行：负责清理死掉的生物和再生库存
     */
    public void tick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            activeOccupants.removeIf(uuid -> {
                Entity entity = level.getEntity(uuid);
                return entity == null || !entity.isAlive();
            });
        }
        // 1. 清理：检查在外活动的生物是否还活着。
        // 如果生物死了或消失了，从活跃列表中移除。
        activeOccupants.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });

        // 2. 再生：只有当 (库存 + 在外活动) < 最大容量时，才开始再生
        if ((currentInside + activeOccupants.size()) < maxOccupants) {
            if (remainingRegenTicks > 0) {
                remainingRegenTicks--;
            } else {
                currentInside++; // 再生一只放入库存
                remainingRegenTicks = regenCooldownTicks;
            }
        }
    }

    /**
     * 释放一只蜘蛛（用于报警触发）
     */
    public Mob releaseOne(ServerLevel level, BlockPos denPos, LivingEntity target) {
        if (currentInside > 0) {
            // 调用内部方法进行生成
            Mob mob = spawnAndTrack(level, denPos);
            if (mob != null) {
                currentInside--; // 只负责库存减一
                if (target != null) {
                    mob.setTarget(target);
                }
                return mob;
            }
        }
        return null;
    }

    /**
     * 释放所有库存蜘蛛（用于夜晚巡逻或遭到攻击）
     */
    public void releaseAll(ServerLevel level, BlockPos denPos, LivingEntity target) {
        int toRelease = currentInside;
        for (int i = 0; i < toRelease; i++) {
            releaseOne(level, denPos, target);
        }
    }

    /**
     * 内部方法：执行物理生成并开始追踪UUID
     */
    private Mob spawnAndTrack(ServerLevel level, BlockPos pos) {
        EntityType<? extends Mob> type = entitySupplier.get();
        Mob mob = type.create(level, EntitySpawnReason.TRIGGERED);

        if (mob != null) {
            // 基础物理位置设置
            mob.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            float yaw = level.random.nextFloat() * 360.0F;
            mob.setYRot(yaw);
            mob.setYBodyRot(yaw);

            // 【核心优化】：不要判断 MonsterSpiderEntity (硬编码)
            // 改为判断接口 Tetherable (通用性)
            if (mob instanceof com.dontstravemc.entity.util.stateutil.Tetherable tetherable) {
                tetherable.setHomePos(pos);
            }

            level.addFreshEntity(mob);
            activeOccupants.add(mob.getUUID());
            return mob;
        }
        return null;
    }

    // --- 属性控制 ---
    public void setMaxOccupants(int max) { this.maxOccupants = max; }
    public void setRegenCooldownSeconds(int seconds) { this.regenCooldownTicks = seconds * 20; }
    public int getActiveCount() { return activeOccupants.size(); }
    public int getCurrentInside() { return currentInside; }

    // --- NBT 适配 ---
    public void toNBT(ValueOutput valueOutput) {
        valueOutput.putInt("MaxOccupants", maxOccupants);
        valueOutput.putInt("CurrentInside", currentInside);
        valueOutput.putInt("RegenCooldownTicks", regenCooldownTicks);
        valueOutput.putInt("RemainingRegenTicks", remainingRegenTicks);
    }

    public void fromNBT(ValueInput valueInput) {
        this.maxOccupants = valueInput.getIntOr("MaxOccupants", 3);
        this.currentInside = valueInput.getIntOr("CurrentInside", 3);
        this.regenCooldownTicks = valueInput.getIntOr("RegenCooldownTicks", 1800);
        this.remainingRegenTicks = valueInput.getIntOr("RemainingRegenTicks", 1800);
    }

    public void occupantReturned(UUID uuid) {
        // 如果该蜘蛛确实是这个巢穴放出去的（在活跃列表中）
        if (activeOccupants.remove(uuid)) {
            currentInside++; // 库存加 1
        }
    }
}