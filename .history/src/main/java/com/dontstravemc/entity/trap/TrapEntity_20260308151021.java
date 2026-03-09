package com.dontstravemc.entity.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class TrapEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final EntityDataAccessor<Boolean> IS_TRIGGERED = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> BAIT_ITEM = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> USES_LEFT = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.INT);
    
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("trap_idle");
    private static final RawAnimation TRIGGER_PRE = RawAnimation.begin().thenPlay("trap_pre");
    private static final RawAnimation TRIGGER_LOOP = RawAnimation.begin().thenLoop("trap_loop");
    
    private UUID capturedEntityUUID;
    private CompoundTag capturedEntityData;
    private boolean hasPlayedTriggerPre = false;

    public TrapEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        // 设置碰撞箱：宽度0.8，高度0.5（陷阱是扁平的）
        this.setBoundingBox(new AABB(
            this.getX() - 0.4, this.getY(), this.getZ() - 0.4,
            this.getX() + 0.4, this.getY() + 0.5, this.getZ() + 0.4
        ));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_TRIGGERED, false);
        builder.define(BAIT_ITEM, ItemStack.EMPTY);
        builder.define(USES_LEFT, 5);
    }
    
    @Override
    public void refreshDimensions() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        super.refreshDimensions();
        // 更新碰撞箱位置
        this.setBoundingBox(new AABB(
            x - 0.4, y, z - 0.4,
            x + 0.4, y + 0.5, z + 0.4
        ));
    }

    @Override
    public void tick() {
        super.tick();
        
        // 应用重力和碰撞检测
        if (!this.level().isClientSide()) {
            // 检查下方是否有方块支撑（检查碰撞箱底部）
            BlockPos below = BlockPos.containing(this.getX(), this.getY() - 0.1, this.getZ());
            boolean hasSupport = this.level().getBlockState(below).isSolid();
            
            if (!hasSupport) {
                // 下方没有方块，应用重力
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));
                this.move(MoverType.SELF, this.getDeltaMovement());
                
                // 如果掉落太快，破坏陷阱
                if (this.getDeltaMovement().y < -2.0) {
                    this.dropAsItem((ServerLevel) this.level());
                    this.discard();
                    return;
                }
            } else {
                // 在地面上，停止垂直移动
                if (this.getDeltaMovement().y < 0) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0, 1.0));
                    // 确保陷阱贴地
                    this.setPos(this.getX(), below.getY() + 1.0, this.getZ());
                }
                // 减少水平移动
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1.0, 0.8));
            }
        }
        
        if (!this.level().isClientSide() && !this.isTriggered()) {
            // 检查是否有生物足够近可以直接捕获（无论是否被诱饵吸引）
            AABB captureBox = this.getBoundingBox().inflate(0.3);
            List<LivingEntity> nearbyForCapture = this.level().getEntitiesOfClass(LivingEntity.class, captureBox, this::canCapture);
            
            if (!nearbyForCapture.isEmpty()) {
                // 捕获最近的一个生物
                LivingEntity closest = nearbyForCapture.get(0);
                double closestDist = closest.distanceTo(this);
                for (LivingEntity entity : nearbyForCapture) {
                    double dist = entity.distanceTo(this);
                    if (dist < closestDist) {
                        closest = entity;
                        closestDist = dist;
                    }
                }
                this.captureEntity(closest);
                return; // 捕获后立即返回
            }
            
            // 如果有诱饵，吸引被诱饵吸引的生物
            if (!this.getBaitItem().isEmpty()) {
                AABB attractBox = this.getBoundingBox().inflate(3.0);
                List<LivingEntity> nearbyForAttract = this.level().getEntitiesOfClass(LivingEntity.class, attractBox, 
                    entity -> this.canCapture(entity) && this.isAttractedByBait(entity));
                
                // 吸引所有被诱饵吸引的生物
                for (LivingEntity target : nearbyForAttract) {
                    if (target instanceof PathfinderMob mob) {
                        mob.getNavigation().moveTo(this.getX(), this.getY(), this.getZ(), 1.0);
                    }
                }
            }
        }
    }
    
    private void dropAsItem(ServerLevel level) {
        int usesLeft = this.getUsesLeft();
        if (usesLeft > 0) {
            ItemStack trapItem = new ItemStack(com.dontstravemc.item.ModItems.TRAP);
            int maxUses = 8;
            int damage = maxUses - usesLeft;
            trapItem.setDamageValue(damage);
            this.spawnAtLocation(level, trapItem);
        }
        
        if (this.isTriggered() && this.capturedEntityData != null) {
            ItemStack spawnEgg = this.createSpawnEggFromEntity();
            if (!spawnEgg.isEmpty()) {
                this.spawnAtLocation(level, spawnEgg);
            }
        }
        
        if (!this.getBaitItem().isEmpty()) {
            this.spawnAtLocation(level, this.getBaitItem().copy());
        }
    }

    private boolean canCapture(LivingEntity entity) {
        if (entity instanceof Player) return false;
        if (this.isTriggered()) return false;
        
        if (entity.isBaby()) return true;
        
        EntityType<?> type = entity.getType();
        String typeName = EntityType.getKey(type).toString();
        
        if (typeName.equals("minecraft:frog")) return true;
        if (typeName.equals("minecraft:axolotl")) return true;
        if (typeName.equals("minecraft:armadillo")) return true;
        if (typeName.equals("minecraft:chicken")) return true;
        if (typeName.equals("minecraft:cat")) return true;
        if (typeName.equals("minecraft:endermite")) return true;
        if (typeName.equals("minecraft:silverfish")) return true;
        if (typeName.equals("dontstravemc:monster_spider")) return true;
        if (typeName.equals("dontstravemc:rabbit")) return true;
        
        return false;
    }
    
    /**
     * 检查生物是否会被当前诱饵吸引
     */
    private boolean isAttractedByBait(LivingEntity entity) {
        ItemStack bait = this.getBaitItem();
        if (bait.isEmpty()) return false;
        
        // 检查生物是否会被这个物品吸引（用于繁殖）
        if (entity instanceof Animal animal) {
            return animal.isFood(bait);
        }
        
        return false;
    }

    private void captureEntity(LivingEntity entity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        this.capturedEntityUUID = entity.getUUID();
        this.capturedEntityData = new CompoundTag();
        
        // Store entity type ID - we only need this for spawn egg creation
        this.capturedEntityData.putString("id", EntityType.getKey(entity.getType()).toString());
        
        entity.discard();
        
        this.setTriggered(true);
        this.hasPlayedTriggerPre = false;
        
        // 减少耐久度
        int usesLeft = this.getUsesLeft();
        if (usesLeft > 0) {
            this.setUsesLeft(usesLeft - 1);
            
            // 如果耐久度用完，破坏陷阱
            if (usesLeft - 1 <= 0) {
                this.dropAsItem(serverLevel);
                this.discard();
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        
        if (!this.level().isClientSide()) {
            if (this.isTriggered()) {
                return InteractionResult.PASS;
            }
            
            if (!heldItem.isEmpty() && this.getBaitItem().isEmpty()) {
                ItemStack bait = heldItem.copy();
                bait.setCount(1);
                this.setBaitItem(bait);
                
                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
                
                return InteractionResult.SUCCESS;
            }
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            // 播放破坏音效（类似船或矿车）
            this.playSound(net.minecraft.sounds.SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
            
            boolean trapCollected = false;
            boolean baitCollected = false;
            boolean capturedCollected = false;
            
            // 1. 优先尝试收回陷阱到背包
            int usesLeft = this.getUsesLeft();
            if (usesLeft > 0) {
                ItemStack trapItem = new ItemStack(com.dontstravemc.item.ModItems.TRAP);
                int maxUses = 8;
                int damage = maxUses - usesLeft;
                trapItem.setDamageValue(damage);
                
                if (player.getInventory().add(trapItem)) {
                    trapCollected = true;
                } else {
                    // 背包满了，掉落陷阱
                    this.spawnAtLocation(level, trapItem);
                }
            }
            
            // 2. 尝试收回捕获的生物
            if (this.isTriggered() && this.capturedEntityData != null) {
                ItemStack spawnEgg = this.createSpawnEggFromEntity();
                if (!spawnEgg.isEmpty()) {
                    if (player.getInventory().add(spawnEgg)) {
                        capturedCollected = true;
                    } else {
                        // 背包满了，掉落生物蛋
                        this.spawnAtLocation(level, spawnEgg);
                    }
                }
            }
            
            // 3. 尝试收回诱饵
            if (!this.getBaitItem().isEmpty()) {
                ItemStack bait = this.getBaitItem().copy();
                if (player.getInventory().add(bait)) {
                    baitCollected = true;
                } else {
                    // 背包满了，掉落诱饵
                    this.spawnAtLocation(level, bait);
                }
            }
            
            this.discard();
            return true;
        }
        return false;
    }

    private ItemStack createSpawnEggFromEntity() {
        if (this.capturedEntityData == null) return ItemStack.EMPTY;
        
        String entityId = this.capturedEntityData.getString("id").orElse("");
        
        return switch (entityId) {
            case "dontstravemc:monster_spider" -> new ItemStack(com.dontstravemc.item.ModItems.MONSTER_SPIDER_SPAWN_EGG);
            case "dontstravemc:rabbit" -> new ItemStack(com.dontstravemc.item.ModItems.DS_RABBIT_SPAWN_EGG);
            case "dontstravemc:butterfly" -> new ItemStack(com.dontstravemc.item.ModItems.BUTTERFLY_SPAWN_EGG);
            case "minecraft:chicken" -> new ItemStack(Items.CHICKEN_SPAWN_EGG);
            case "minecraft:cat" -> new ItemStack(Items.CAT_SPAWN_EGG);
            case "minecraft:frog" -> new ItemStack(Items.FROG_SPAWN_EGG);
            case "minecraft:axolotl" -> new ItemStack(Items.AXOLOTL_SPAWN_EGG);
            case "minecraft:armadillo" -> new ItemStack(Items.ARMADILLO_SPAWN_EGG);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        this.setTriggered(tag.getBooleanOr("Triggered", false));
        this.setUsesLeft(tag.getIntOr("UsesLeft", 5));
        
        tag.child("BaitItem").ifPresent(baitInput -> {
            baitInput.read("item", ItemStack.CODEC).ifPresent(this::setBaitItem);
        });
        
        tag.child("CapturedEntity").ifPresent(entityInput -> {
            this.capturedEntityData = new CompoundTag();
            // Store the data - we'll need to manually reconstruct it
            entityInput.getString("id").ifPresent(id -> this.capturedEntityData.putString("id", id));
        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.putBoolean("Triggered", this.isTriggered());
        tag.putInt("UsesLeft", this.getUsesLeft());
        
        if (!this.getBaitItem().isEmpty()) {
            ValueOutput baitOutput = tag.child("BaitItem");
            baitOutput.store("item", ItemStack.CODEC, this.getBaitItem());
        }
        
        if (this.capturedEntityData != null) {
            ValueOutput entityOutput = tag.child("CapturedEntity");
            this.capturedEntityData.getString("id").ifPresent(id -> entityOutput.putString("id", id));
        }
    }

    public boolean isTriggered() {
        return this.entityData.get(IS_TRIGGERED);
    }

    public void setTriggered(boolean triggered) {
        this.entityData.set(IS_TRIGGERED, triggered);
    }

    public ItemStack getBaitItem() {
        return this.entityData.get(BAIT_ITEM);
    }

    public void setBaitItem(ItemStack item) {
        this.entityData.set(BAIT_ITEM, item);
    }

    public int getUsesLeft() {
        return this.entityData.get(USES_LEFT);
    }

    public void setUsesLeft(int uses) {
        this.entityData.set(USES_LEFT, uses);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TrapEntity> controller = new AnimationController<>("controller", 5, state -> {
            if (this.isTriggered()) {
                if (!this.hasPlayedTriggerPre) {
                    this.hasPlayedTriggerPre = true;
                    return state.setAndContinue(TRIGGER_PRE);
                }
                return state.setAndContinue(TRIGGER_LOOP);
            }
            return state.setAndContinue(IDLE_ANIM);
        });
        
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
