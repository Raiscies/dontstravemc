package com.dontstravemc.entity.animal;

import com.dontstravemc.entity.util.stateutil.Eater;
import com.dontstravemc.entity.util.stateutil.Tetherable;
import com.dontstravemc.entity.component.HomeTetherComponent;
import com.dontstravemc.entity.ai.goal.target.ReturnToDenGoal;
import com.dontstravemc.entity.ai.goal.target.EatItemGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("rabbit_idle");
    private static final RawAnimation WATCH_ANIM = RawAnimation.begin().thenLoop("rabbit_watch");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("rabbit_run");
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenLoop("rabbit_eat");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("rabbit_idle");

    public RabbitEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ReturnToDenGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new EatItemGoal(this, 1.0D, stack -> stack.has(DataComponents.FOOD)));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (this.isEating()) {
                return state.setAndContinue(EAT_ANIM);
            }
            if (state.isMoving()) {
                state.setControllerSpeed(1.5f);
                return state.setAndContinue(RUN_ANIM);
            }
            if (this.isAlert()) {
                return state.setAndContinue(WATCH_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_ALERT, false);
        builder.define(IS_EATING, false);
        builder.define(EATING_ITEM, ItemStack.EMPTY);
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

    // Tetherable implementation
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
        return time >= 12000 && time <= 23000; // Dusk and Night
    }

    // Eater implementation
    @Override
    public void setEating(boolean eating) { this.entityData.set(IS_EATING, eating); }

    public boolean isEating() { return this.entityData.get(IS_EATING); }

    @Override
    public void setEatingItem(ItemStack stack) { this.entityData.set(EATING_ITEM, stack); }

    @Override
    public SoundEvent getEatingSound() { return net.minecraft.sounds.SoundEvents.RABBIT_EAT; }

    public boolean isAlert() { return this.entityData.get(IS_ALERT); }

    public void setAlert(boolean alert) { this.entityData.set(IS_ALERT, alert); }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
