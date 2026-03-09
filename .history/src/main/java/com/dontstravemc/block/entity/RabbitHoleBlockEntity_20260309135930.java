package com.dontstravemc.block.entity;

import com.dontstravemc.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 兔子洞方块实体 - 参考原版蜂巢设计
 * 负责存储和管理兔子的进出
 */
public class RabbitHoleBlockEntity extends BlockEntity {
    private static final int MAX_OCCUPANTS = 1; // 最多容纳1只兔子
    private static final int MIN_OCCUPATION_TICKS = 2400; // 最少停留时间：2分钟（2400 ticks）
    
    private final List<OccupantData> stored = new ArrayList<>();
    private int regenCooldown = 0; // 再生冷却时间
    private static final int REGEN_COOLDOWN_TICKS = 2400; // 再生冷却：2分钟

    public RabbitHoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RABBIT_HOLE_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // 保存兔子数据
        CompoundTag rabbitsTag = new CompoundTag();
        rabbitsTag.putInt("Count", stored.size());
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag rabbitTag = new CompoundTag();
            stored.get(i).save(rabbitTag);
            rabbitsTag.put("Rabbit" + i, rabbitTag);
        }
        tag.put("Rabbits", rabbitsTag);
        
        // 保存再生冷却
        tag.putInt("RegenCooldown", regenCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // 加载兔子数据
        stored.clear();
        if (tag.contains("Rabbits")) {
            CompoundTag rabbitsTag = tag.getCompound("Rabbits");
            int count = rabbitsTag.getInt("Count");
            for (int i = 0; i < count; i++) {
                if (rabbitsTag.contains("Rabbit" + i)) {
                    CompoundTag rabbitTag = rabbitsTag.getCompound("Rabbit" + i);
                    OccupantData data = new OccupantData();
                    data.load(rabbitTag);
                    stored.add(data);
                }
            }
        }
        
        // 加载再生冷却
        regenCooldown = tag.getInt("RegenCooldown");
    }

    /**
     * 服务端Tick - 处理兔子释放和再生
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RabbitHoleBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        // 1. 处理已存储兔子的释放
        blockEntity.tickOccupants(serverLevel, pos);
        
        // 2. 处理兔子再生
        blockEntity.tickRegeneration(serverLevel);
    }

    /**
     * Tick已存储的兔子，检查是否应该释放
     */
    private void tickOccupants(ServerLevel level, BlockPos pos) {
        boolean changed = false;
        Iterator<OccupantData> iterator = stored.iterator();
        
        while (iterator.hasNext()) {
            OccupantData data = iterator.next();
            data.ticksInHole++;
            
            // 检查是否应该释放
            if (data.ticksInHole >= MIN_OCCUPATION_TICKS) {
                // 只在白天且没有玩家在附近时释放
                if (shouldReleaseRabbit(level, pos)) {
                    if (releaseRabbit(level, pos, data)) {
                        iterator.remove();
                        changed = true;
                    }
                }
            }
        }
        
        if (changed) {
            setChanged();
        }
    }

    /**
     * 检查是否应该释放兔子
     */
    private boolean shouldReleaseRabbit(ServerLevel level, BlockPos pos) {
        // 必须是白天
        if (!level.isDay()) {
            return false;
        }
        
        // 检查10格内是否有玩家
        boolean hasNearbyPlayer = level.getNearestPlayer(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            10.0, false
        ) != null;
        
        return !hasNearbyPlayer;
    }

    /**
     * 释放一只兔子到世界中
     */
    private boolean releaseRabbit(ServerLevel level, BlockPos pos, OccupantData data) {
        Rabbit rabbit = EntityType.RABBIT.create(level, EntitySpawnReason.TRIGGERED);
        if (rabbit == null) {
            return false;
        }
        
        // 设置位置（在洞口上方）
        rabbit.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        // 随机朝向
        float yaw = level.random.nextFloat() * 360.0F;
        rabbit.setYRot(yaw);
        rabbit.setYBodyRot(yaw);
        
        // 恢复兔子的年龄数据
        if (data.age != 0) {
            rabbit.setAge(data.age);
        }
        
        // 添加到世界
        return level.addFreshEntity(rabbit);
    }

    /**
     * Tick再生系统
     */
    private void tickRegeneration(ServerLevel level) {
        // 如果已经满了，不需要再生
        if (stored.size() >= MAX_OCCUPANTS) {
            regenCooldown = REGEN_COOLDOWN_TICKS; // 重置冷却
            return;
        }
        
        // 冷却计时
        if (regenCooldown > 0) {
            regenCooldown--;
        } else {
            // 冷却结束，生成新兔子
            addNewRabbit();
            regenCooldown = REGEN_COOLDOWN_TICKS; // 重置冷却
            setChanged();
        }
    }

    /**
     * 添加一只新兔子到存储中
     */
    private void addNewRabbit() {
        if (stored.size() < MAX_OCCUPANTS) {
            OccupantData data = new OccupantData();
            data.ticksInHole = 0;
            data.age = 0; // 成年兔子
            stored.add(data);
        }
    }

    /**
     * 兔子进入洞穴（当兔子主动返回时调用）
     */
    public void addOccupant(Rabbit rabbit) {
        if (stored.size() < MAX_OCCUPANTS) {
            OccupantData data = new OccupantData();
            data.ticksInHole = 0;
            data.age = rabbit.getAge();
            
            stored.add(data);
            rabbit.discard(); // 移除实体
            
            setChanged();
        }
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return stored.isEmpty();
    }

    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return stored.size() >= MAX_OCCUPANTS;
    }

    /**
     * 获取当前容纳数量
     */
    public int getOccupantCount() {
        return stored.size();
    }

    /**
     * 强制释放所有兔子（当方块被破坏时）
     */
    public void releaseAllRabbits(ServerLevel level, BlockPos pos) {
        for (OccupantData data : stored) {
            releaseRabbit(level, pos, data);
        }
        stored.clear();
        setChanged();
    }

    /**
     * 存储单个兔子的数据
     */
    private static class OccupantData {
        int ticksInHole = 0; // 在洞中的时间
        int age = 0; // 兔子年龄（负数=幼年，0=成年）
        
        void save(CompoundTag tag) {
            tag.putInt("TicksInHole", ticksInHole);
            tag.putInt("Age", age);
        }
        
        void load(CompoundTag tag) {
            ticksInHole = tag.getInt("TicksInHole");
            age = tag.getInt("Age");
        }
    }
}
