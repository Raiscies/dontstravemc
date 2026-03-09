package com.dontstravemc.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.*;
import java.util.EnumSet;

public class RabbitEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Boolean> IS_SLEEPING = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_LOOKING = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_FLEEING = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_HIT = SynchedEntityData.defineId(RabbitEntity.class, EntityDataSerializers.BOOLEAN);

    private int idleCount = 0;
    private int idle2Threshold = 5;
    private int hitTimer = 0;

    private static final RawAnimation IDLE = RawAnimation.begin().thenPlay("idle");
    private static final RawAnimation IDLE2 = RawAnimation.begin().thenPlay("idle2");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation LOOK_PRE = RawAnimation.begin().thenPlay("look_pre").thenLoop("look_loop");
    private static final RawAnimation EAT = RawAnimation.begin().thenLoop("eat");
    private static final RawAnimation HIT = RawAnimation.begin().thenPlay("hit");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenPlay("sleep_pre").thenLoop("sleep_loop");
    private static final RawAnimation WAKE = RawAnimation.begin().thenPlay("sleep_pst");

    public RabbitEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_SLEEPING, false);
        builder.define(IS_EATING, false);
        builder.define(IS_LOOKING, false);
        builder.define(IS_FLEEING, false);
        builder.define(IS_HIT, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public void start() {
                super.start();
                RabbitEntity.this.entityData.set(IS_FLEEING, true);
            }
            @Override
            public void stop() {
                super.stop();
                RabbitEntity.this.entityData.set(IS_FLEEING, false);
            }
        });
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            Player player = this.level().getNearestPlayer(this, 10.0D);
            this.entityData.set(IS_LOOKING, player != null && !this.entityData.get(IS_FLEEING) && !this.entityData.get(IS_SLEEPING));
            
            if (this.hitTimer > 0) {
                this.hitTimer--;
                if (this.hitTimer == 0) {
                    this.entityData.set(IS_HIT, false);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && amount > 0) {
            this.entityData.set(IS_HIT, true);
            this.hitTimer = 10;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH);
            }
            if (this.entityData.get(IS_HIT)) {
                return state.setAndContinue(HIT);
            }
            if (this.entityData.get(IS_SLEEPING)) {
                return state.setAndContinue(SLEEP);
            }
            if (this.entityData.get(IS_EATING)) {
                return state.setAndContinue(EAT);
            }
            if (this.entityData.get(IS_FLEEING)) {
                return state.setAndContinue(RUN);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            if (this.entityData.get(IS_LOOKING)) {
                return state.setAndContinue(LOOK_PRE);
            }

            // Idle cycles
            if (state.getController().getAnimationState() == AnimationState.STOPPED) {
                idleCount++;
                if (idleCount >= idle2Threshold) {
                    idleCount = 0;
                    idle2Threshold = 5 + this.random.nextInt(6);
                    return state.setAndContinue(IDLE2);
                }
                return state.setAndContinue(IDLE);
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(IS_SLEEPING, sleeping);
    }

    public boolean isSleepingDs() {
        return this.entityData.get(IS_SLEEPING);
    }
}
