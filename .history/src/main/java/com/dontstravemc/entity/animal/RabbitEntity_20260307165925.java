package com.dontstravemc.entity.animal;

import com.dontstravemc.entity.util.stateutil.Eater;
import com.dontstravemc.entity.util.stateutil.Tetherable;
import com.dontstravemc.entity.component.HomeTetherComponent;
import com.dontstravemc.entity.ai.goal.target.ReturnToDenGoal;
import com.dontstravemc.entity.ai.goal.target.EatItemGoal;
import com.dontstravemc.entity.ai.goal.animal.RabbitAvoidPlayerGoal;
import com.dontstravemc.entity.ai.goal.animal.RabbitSocialPanicGoal;
import com.dontstravemc.entity.ai.goal.animal.RabbitFindHoleGoal;
import com.dontstravemc.entity.ai.goal.animal.RabbitStrollAroundHomeGoal;
import com.dontstravemc.event.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RabbitEntity extends PathfinderMob implements GeoEntity, Eater, Tetherable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HomeTetherComponent tether = new HomeTetherComponent(8, 24, 32);

    private static final EntityDataAccessor<Boolean> IS_ALERT = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> EATING_ITEM = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> SLEEP_PHASE = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT_TICKS = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE2_ANIM = RawAnimation.begin().thenPlay("idle2");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenLoop("eat");
    private static final RawAnimation LOOK_PRE = RawAnimation.begin().thenPlay("look_pre");
    private static final RawAnimation LOOK_LOOP = RawAnimation.begin().thenLoop("look_loop");
    private static final RawAnimation HIT_ANIM = RawAnimation.begin().thenPlay("hit");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SLEEP_PRE = RawAnimation.begin().thenPlay("sleep_pre");
    private static final RawAnimation SLEEP_LOOP = RawAnimation.begin().thenLoop("sleep_pre");
    private static final RawAnimation SLEEP_PST = RawAnimation.begin().thenPlay("sleep_pst");

    private int idleCount = 0;
    private int nextSpecialIdle = 5;
    private int sleepTick = 0;
    private int lookPreTick = -100;
    private boolean wasAlert = false;
    private boolean wasMoving = false;
    private int stepSoundCooldown = 0;

    public RabbitEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.nextSpecialIdle = 5 + this.random.nextInt(6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RabbitSocialPanicGoal(this));
        this.goalSelector.addGoal(2, new ReturnToDenGoal(this, 1.5D));
        this.goalSelector.addGoal(3, new RabbitAvoidPlayerGoal(this, 1.5D));
        this.goalSelector.addGoal(4, new EatItemGoal(this, 1.0D, stack -> 
                isVegetarian(stack)));
        this.goalSelector.addGoal(5, new RabbitFindHoleGoal(this));
        this.goalSelector.addGoal(6, new RabbitStrollAroundHomeGoal(this, 0.8D, 8));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    private boolean isVegetarian(ItemStack stack) {
        if (!stack.has(DataComponents.FOOD)) return false;
        Item item = stack.getItem();
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        return name.contains("berry") || name.contains("carrot") || name.contains("seed") || name.contains("apple");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<RabbitEntity> controller = new AnimationController<>("controller", 5, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIM);
            }

            if (this.entityData.get(HIT_TICKS) > 0) {
                return state.setAndContinue(HIT_ANIM);
            }

            int sleepPhase = this.entityData.get(SLEEP_PHASE);
            if (sleepPhase == 2) return state.setAndContinue(SLEEP_LOOP);
            if (sleepPhase == 1) return state.setAndContinue(SLEEP_PRE);
            if (sleepPhase == 3) return state.setAndContinue(SLEEP_PST);

            if (this.isEating()) {
                return state.setAndContinue(EAT_ANIM);
            }

            if (state.isMoving()) {
                if (!this.wasMoving) {
                    this.idleCount = 0;
                }
                this.wasMoving = true;
                
                if (this.isAlert() || this.getLastHurtByMob() != null) {
                    state.setControllerSpeed(1.5f);
                    return state.setAndContinue(RUN_ANIM);
                }
                state.setControllerSpeed(1.0f);
                return state.setAndContinue(WALK_ANIM);
            }

            this.wasMoving = false;

            if (this.isAlert()) {
                if ((this.tickCount - this.lookPreTick) < 15) {
                    return state.setAndContinue(LOOK_PRE);
                }
                return state.setAndContinue(LOOK_LOOP);
            }

            if (!state.isMoving() && this.tickCount % 200 == 0) {
                this.idleCount++;
                
                if (this.idleCount >= this.nextSpecialIdle) {
                    this.idleCount = 0;
                    this.nextSpecialIdle = 5 + this.random.nextInt(6);
                    return state.setAndContinue(IDLE2_ANIM);
                }
            }

            return state.setAndContinue(IDLE_ANIM);
        });
        
        controllers.add(controller);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        
        // Play step sounds when moving
        if (!this.level().isClientSide() && this.onGround()) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0001) {
                if (this.stepSoundCooldown <= 0) {
                    this.playSound(ModSounds.RABBIT_HOP, 0.15F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
                    this.stepSoundCooldown = this.isAlert() ? 8 : 12; // Faster steps when running
                }
            }
            
            if (this.stepSoundCooldown > 0) {
                this.stepSoundCooldown--;
            }
        }
        
        if (!this.level().isClientSide()) {
            int hitTicks = this.entityData.get(HIT_TICKS);
            if (hitTicks > 0) {
                this.entityData.set(HIT_TICKS, hitTicks - 1);
            }

            int phase = this.entityData.get(SLEEP_PHASE);
            boolean shouldBeSleeping = shouldReturnHome() && !hasHome(); 
            
            if (shouldBeSleeping && phase == 0) {
                this.entityData.set(SLEEP_PHASE, 1);
                this.sleepTick = this.tickCount;
            } else if (!shouldBeSleeping && (phase == 1 || phase == 2)) {
                this.entityData.set(SLEEP_PHASE, 3);
                this.sleepTick = this.tickCount;
            }
            
            if (phase == 1 && (this.tickCount - this.sleepTick) > 20) this.entityData.set(SLEEP_PHASE, 2);
            if (phase == 3 && (this.tickCount - this.sleepTick) > 20) this.entityData.set(SLEEP_PHASE, 0);

            if (this.isAlert() && !this.wasAlert) {
                this.lookPreTick = this.tickCount;
                // Play scream sound when becoming alert
                this.playSound(ModSounds.RABBIT_SCREAM, 0.8F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
            }
            this.wasAlert = this.isAlert();
        }
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel world, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(world, source, amount);
        if (hurt) {
            this.entityData.set(HIT_TICKS, 10);
        }
        return hurt;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RABBIT_SCREAM;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_ALERT, false);
        builder.define(IS_EATING, false);
        builder.define(EATING_ITEM, ItemStack.EMPTY);
        builder.define(SLEEP_PHASE, 0);
        builder.define(HIT_TICKS, 0);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        this.tether.toNBT(valueOutput);
    }

    @Override
    public void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        this.tether.fromNBT(valueInput);
    }

    @Override
    public HomeTetherComponent getTether() { return this.tether; }

    @Override
    public void setHomePos(BlockPos pos) { this.tether.setHomePos(pos); }

    @Override
    public boolean hasHome() { return this.tether.getHomePos() != null; }

    @Override
    public boolean shouldReturnHome() {
        if (this.getTarget() != null) return false;
        long time = this.level().getDayTime() % 24000;
        return time >= 12000 && time <= 23000;
    }

    @Override
    public void setEating(boolean eating) { this.entityData.set(IS_EATING, eating); }

    public boolean isEating() { return this.entityData.get(IS_EATING); }

    @Override
    public void setEatingItem(ItemStack stack) { this.entityData.set(EATING_ITEM, stack); }

    @Override
    public SoundEvent getEatingSound() { return null; }

    public boolean isAlert() { return this.entityData.get(IS_ALERT); }

    public void setAlert(boolean alert) { this.entityData.set(IS_ALERT, alert); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
