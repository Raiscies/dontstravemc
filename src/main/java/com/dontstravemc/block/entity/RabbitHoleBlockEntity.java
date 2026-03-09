package com.dontstravemc.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 兔子洞方块实体 - 完全参考原版 BeehiveBlockEntity 设计
 */
public class RabbitHoleBlockEntity extends BlockEntity {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String RABBITS = "rabbits";
    
    // 忽略的兔子NBT标签（类似蜜蜂）
    static final List<String> IGNORED_RABBIT_TAGS = Arrays.asList(
        "Air",
        "drop_chances",
        "equipment",
        "Brain",
        "CanPickUpLoot",
        "DeathTime",
        "fall_distance",
        "FallFlying",
        "Fire",
        "HurtByTimestamp",
        "HurtTime",
        "LeftHanded",
        "Motion",
        "NoGravity",
        "OnGround",
        "PortalCooldown",
        "Pos",
        "Rotation",
        "sleeping_pos",
        "Passengers",
        "leash",
        "UUID"
    );
    
    public static final int MAX_OCCUPANTS = 1;
    public static final int MIN_OCCUPATION_TICKS = 0; // 立即释放，无需等待
    private final List<RabbitHoleBlockEntity.RabbitData> stored = Lists.newArrayList();
    
    // 再生系统 - 2.5天 = 50分钟 = 60000 ticks
    private int regenCooldown = 0;
    private static final int REGEN_COOLDOWN_TICKS = 60000; // 50分钟（2.5天）
    private boolean initialized = false; // 标记是否已初始化

    public RabbitHoleBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.RABBIT_HOLE_BLOCK_ENTITY, blockPos, blockState);
    }
    
    /**
     * 初始化时生成一只兔子
     */
    private void initializeWithRabbit() {
        if (!initialized && stored.isEmpty()) {
            storeRabbit(RabbitHoleBlockEntity.Occupant.create(0));
            initialized = true;
            setChanged();
        }
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() >= MAX_OCCUPANTS;
    }

    public int getOccupantCount() {
        return this.stored.size();
    }

    /**
     * 兔子进入洞穴（参考 BeehiveBlockEntity.addOccupant）
     * 支持自定义兔子实体和原版兔子
     */
    public void addOccupant(Entity entity) {
        if (this.stored.size() < MAX_OCCUPANTS) {
            // 支持自定义兔子实体
            if (entity instanceof com.dontstravemc.entity.animal.RabbitEntity customRabbit) {
                customRabbit.stopRiding();
                customRabbit.ejectPassengers();
                customRabbit.dropLeash();
                this.storeRabbit(RabbitHoleBlockEntity.Occupant.of(customRabbit));
                
                // 播放进入音效（可选）
                if (this.level != null) {
                    BlockPos blockPos = this.getBlockPos();
                    // 可以添加音效：this.level.playSound(null, blockPos, SoundEvents.xxx, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                
                customRabbit.discard();
                this.setChanged();
            }
            // 向后兼容原版兔子
            else if (entity instanceof Rabbit rabbit) {
                rabbit.stopRiding();
                rabbit.ejectPassengers();
                rabbit.dropLeash();
                this.storeRabbit(RabbitHoleBlockEntity.Occupant.of(rabbit));
                
                if (this.level != null) {
                    BlockPos blockPos = this.getBlockPos();
                }
                
                rabbit.discard();
                this.setChanged();
            }
        }
    }

    public void storeRabbit(RabbitHoleBlockEntity.Occupant occupant) {
        this.stored.add(new RabbitHoleBlockEntity.RabbitData(occupant));
    }

    /**
     * 释放所有兔子（当方块被破坏时）
     */
    public void emptyAllRabbits(@Nullable Player player, BlockState blockState, RabbitHoleBlockEntity.RabbitReleaseStatus releaseStatus) {
        List<Entity> list = this.releaseAllOccupants(blockState, releaseStatus);
        if (!list.isEmpty()) {
            this.setChanged();
        }
    }

    private List<Entity> releaseAllOccupants(BlockState blockState, RabbitHoleBlockEntity.RabbitReleaseStatus releaseStatus) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf(rabbitData -> 
            releaseOccupant(this.level, this.worldPosition, blockState, rabbitData.toOccupant(), list, releaseStatus)
        );
        if (!list.isEmpty()) {
            this.setChanged();
        }
        return list;
    }

    /**
     * 释放单个兔子
     */
    private static boolean releaseOccupant(
        Level level,
        BlockPos blockPos,
        BlockState blockState,
        RabbitHoleBlockEntity.Occupant occupant,
        @Nullable List<Entity> list,
        RabbitHoleBlockEntity.RabbitReleaseStatus releaseStatus
    ) {
        // 检查释放条件
        if (releaseStatus != RabbitHoleBlockEntity.RabbitReleaseStatus.EMERGENCY) {
            // 检查是否是夜晚或下雨（参考 Bee.isNightOrRaining）
            if (isNightOrRaining(level)) {
                return false;
            }
            
            // 检查附近是否有玩家
            boolean hasNearbyPlayer = level.getNearestPlayer(
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5,
                10.0, false
            ) != null;
            
            if (hasNearbyPlayer) {
                return false;
            }
        }

        Entity entity = occupant.createEntity(level, blockPos);
        if (entity != null) {
            // 【关键】设置兔子的家位置，确保它知道要回到这个洞穴
            if (entity instanceof com.dontstravemc.entity.util.stateutil.Tetherable tetherable) {
                tetherable.setHomePos(blockPos);
            }
            
            if (list != null) {
                list.add(entity);
            }

            // 设置生成位置（使用 snapTo 而不是 moveTo）
            double x = blockPos.getX() + 0.5;
            double y = blockPos.getY() + 0.5;
            double z = blockPos.getZ() + 0.5;
            entity.snapTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);

            return level.addFreshEntity(entity);
        } else {
            return false;
        }
    }
    
    /**
     * 检查是否是夜晚或下雨（参考 Bee.isNightOrRaining）
     */
    private static boolean isNightOrRaining(Level level) {
        return level.dimensionType().hasSkyLight() && level.dimension() != Level.END && (level.isDarkOutside() || level.isRaining());
    }

    /**
     * Tick存储的兔子
     */
    private static void tickOccupants(Level level, BlockPos blockPos, BlockState blockState, List<RabbitHoleBlockEntity.RabbitData> list) {
        boolean changed = false;
        Iterator<RabbitHoleBlockEntity.RabbitData> iterator = list.iterator();

        while (iterator.hasNext()) {
            RabbitHoleBlockEntity.RabbitData rabbitData = iterator.next();
            if (rabbitData.tick()) {
                if (releaseOccupant(level, blockPos, blockState, rabbitData.toOccupant(), null, RabbitHoleBlockEntity.RabbitReleaseStatus.RABBIT_RELEASED)) {
                    changed = true;
                    iterator.remove();
                }
            }
        }

        if (changed) {
            setChanged(level, blockPos, blockState);
        }
    }

    /**
     * 服务端Tick
     */
    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, RabbitHoleBlockEntity blockEntity) {
        // 首次初始化：生成一只兔子
        blockEntity.initializeWithRabbit();
        
        // Tick已存储的兔子
        tickOccupants(level, blockPos, blockState, blockEntity.stored);
        
        // 处理再生
        if (blockEntity.stored.size() < MAX_OCCUPANTS) {
            if (blockEntity.regenCooldown > 0) {
                blockEntity.regenCooldown--;
            } else {
                // 再生一只新兔子
                blockEntity.storeRabbit(RabbitHoleBlockEntity.Occupant.create(0));
                blockEntity.regenCooldown = REGEN_COOLDOWN_TICKS;
                blockEntity.setChanged();
            }
        } else {
            blockEntity.regenCooldown = REGEN_COOLDOWN_TICKS;
        }
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        this.stored.clear();
        List<RabbitHoleBlockEntity.Occupant> list = valueInput.read("rabbits", RabbitHoleBlockEntity.Occupant.LIST_CODEC).orElse(List.of());
        list.forEach(this::storeRabbit);
        this.regenCooldown = valueInput.getIntOr("RegenCooldown", REGEN_COOLDOWN_TICKS);
        this.initialized = valueInput.getBooleanOr("Initialized", false);
    }

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        valueOutput.store("rabbits", RabbitHoleBlockEntity.Occupant.LIST_CODEC, this.getRabbits());
        valueOutput.putInt("RegenCooldown", this.regenCooldown);
        valueOutput.putBoolean("Initialized", this.initialized);
    }

    private List<RabbitHoleBlockEntity.Occupant> getRabbits() {
        return this.stored.stream().map(RabbitHoleBlockEntity.RabbitData::toOccupant).toList();
    }

    /**
     * 兔子数据（内部类）
     */
    static class RabbitData {
        private final RabbitHoleBlockEntity.Occupant occupant;
        private int ticksInHole;

        RabbitData(RabbitHoleBlockEntity.Occupant occupant) {
            this.occupant = occupant;
            this.ticksInHole = occupant.ticksInHole();
        }

        public boolean tick() {
            return this.ticksInHole++ > this.occupant.minTicksInHole;
        }

        public RabbitHoleBlockEntity.Occupant toOccupant() {
            return new RabbitHoleBlockEntity.Occupant(this.occupant.entityData, this.ticksInHole, this.occupant.minTicksInHole);
        }
    }

    /**
     * 释放状态枚举
     */
    public enum RabbitReleaseStatus {
        RABBIT_RELEASED,
        EMERGENCY
    }

    /**
     * 兔子占用者记录（完全参考 BeehiveBlockEntity.Occupant）
     */
    public record Occupant(TypedEntityData<EntityType<?>> entityData, int ticksInHole, int minTicksInHole) {
        public static final Codec<RabbitHoleBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                TypedEntityData.codec(EntityType.CODEC).fieldOf("entity_data").forGetter(RabbitHoleBlockEntity.Occupant::entityData),
                Codec.INT.fieldOf("ticks_in_hole").forGetter(RabbitHoleBlockEntity.Occupant::ticksInHole),
                Codec.INT.fieldOf("min_ticks_in_hole").forGetter(RabbitHoleBlockEntity.Occupant::minTicksInHole)
            ).apply(instance, RabbitHoleBlockEntity.Occupant::new)
        );
        
        public static final Codec<List<RabbitHoleBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
        
        public static final StreamCodec<RegistryFriendlyByteBuf, RabbitHoleBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
            TypedEntityData.streamCodec(EntityType.STREAM_CODEC),
            RabbitHoleBlockEntity.Occupant::entityData,
            ByteBufCodecs.VAR_INT,
            RabbitHoleBlockEntity.Occupant::ticksInHole,
            ByteBufCodecs.VAR_INT,
            RabbitHoleBlockEntity.Occupant::minTicksInHole,
            RabbitHoleBlockEntity.Occupant::new
        );

        /**
         * 从实体创建 Occupant
         */
        public static RabbitHoleBlockEntity.Occupant of(Entity entity) {
            RabbitHoleBlockEntity.Occupant result;
            try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), RabbitHoleBlockEntity.LOGGER)) {
                TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, entity.registryAccess());
                entity.save(tagValueOutput);
                RabbitHoleBlockEntity.IGNORED_RABBIT_TAGS.forEach(tagValueOutput::discard);
                CompoundTag compoundTag = tagValueOutput.buildResult();
                result = new RabbitHoleBlockEntity.Occupant(
                    TypedEntityData.of(entity.getType(), compoundTag), 
                    0, 
                    MIN_OCCUPATION_TICKS
                );
            }
            return result;
        }

        /**
         * 创建默认兔子（使用自定义兔子类型）
         */
        public static RabbitHoleBlockEntity.Occupant create(int ticksInHole) {
            return new RabbitHoleBlockEntity.Occupant(
                TypedEntityData.of(com.dontstravemc.entity.ModEntities.RABBIT, new CompoundTag()), 
                ticksInHole, 
                MIN_OCCUPATION_TICKS
            );
        }

        /**
         * 从数据创建实体
         */
        @Nullable
        public Entity createEntity(Level level, BlockPos blockPos) {
            CompoundTag compoundTag = this.entityData.copyTagWithoutId();
            RabbitHoleBlockEntity.IGNORED_RABBIT_TAGS.forEach(compoundTag::remove);
            Entity entity = EntityType.loadEntityRecursive(
                this.entityData.type(), 
                compoundTag, 
                level, 
                EntitySpawnReason.LOAD, 
                entityx -> entityx
            );
            
            if (entity != null) {
                entity.setNoGravity(false);
                // 自定义兔子不需要额外的释放数据处理（没有年龄、繁殖等）
                // 原版兔子需要处理年龄和繁殖时间
                if (entity instanceof Rabbit rabbit) {
                    setRabbitReleaseData(this.ticksInHole, rabbit);
                }
                return entity;
            }
            
            return null;
        }

        /**
         * 设置兔子释放数据（调整年龄等）
         */
        private static void setRabbitReleaseData(int ticksInHole, Rabbit rabbit) {
            int age = rabbit.getAge();
            if (age < 0) {
                rabbit.setAge(Math.min(0, age + ticksInHole));
            } else if (age > 0) {
                rabbit.setAge(Math.max(0, age - ticksInHole));
            }
            rabbit.setInLoveTime(Math.max(0, rabbit.getInLoveTime() - ticksInHole));
        }
    }
}
