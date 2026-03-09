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
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IS_TRIGGERED, false);
        builder.define(BAIT_ITEM, ItemStack.EMPTY);
        builder.define(USES_LEFT, 5);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide() && !this.isTriggered()) {
            AABB searchBox = this.getBoundingBox().inflate(0.5);
            List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox, this::canCapture);
            
            if (!nearbyEntities.isEmpty()) {
                LivingEntity target = nearbyEntities.get(0);
                this.captureEntity(target);
            }
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

    private void captureEntity(LivingEntity entity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        this.capturedEntityUUID = entity.getUUID();
        this.capturedEntityData = new CompoundTag();
        
        // Store entity type ID - we only need this for spawn egg creation
        this.capturedEntityData.putString("id", EntityType.getKey(entity.getType()).toString());
        
        entity.discard();
        
        this.setTriggered(true);
        this.hasPlayedTriggerPre = false;
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
            if (this.isTriggered() && this.capturedEntityData != null) {
                this.dropCapturedEntity(player, level);
            }
            
            if (!this.getBaitItem().isEmpty()) {
                this.spawnAtLocation(level, this.getBaitItem());
            }
            
            this.dropTrapItem(level);
            
            this.discard();
            return true;
        }
        return false;
    }

    private void dropCapturedEntity(Player player, ServerLevel level) {
        if (this.capturedEntityData == null) return;
        
        ItemStack spawnEgg = this.createSpawnEggFromEntity();
        if (!spawnEgg.isEmpty()) {
            if (!player.getInventory().add(spawnEgg)) {
                this.spawnAtLocation(level, spawnEgg);
            }
            
            int uses = this.getUsesLeft() - 1;
            this.setUsesLeft(uses);
        }
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

    private void dropTrapItem(ServerLevel level) {
        int usesLeft = this.getUsesLeft();
        
        if (usesLeft <= 0) {
            return;
        }
        
        ItemStack trapItem = new ItemStack(com.dontstravemc.item.ModItems.TRAP);
        
        int maxUses = 5;
        int damage = maxUses - usesLeft;
        
        trapItem.setDamageValue(damage);
        
        this.spawnAtLocation(level, trapItem);
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
