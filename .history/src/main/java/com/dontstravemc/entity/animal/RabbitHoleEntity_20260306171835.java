package com.dontstravemc.entity.animal;

import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.component.DenSpawnerComponent;
import com.dontstravemc.entity.util.stateutil.Den;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RabbitHoleEntity extends Mob implements GeoEntity, Den {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DenSpawnerComponent spawner;

    public RabbitHoleEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        // Each hole can hold 1 rabbit, regenerates every 2 minutes
        this.spawner = new DenSpawnerComponent(() -> ModEntities.RABBIT, 1, 120);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void occupantReturned(Mob mob) {
        if (!this.level().isClientSide()) {
            this.spawner.occupantReturned(mob.getUUID());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        
        ServerLevel serverLevel = (ServerLevel) this.level();
        this.spawner.tick(serverLevel, this.blockPosition());

        // Release at daybreak
        if (serverLevel.isBrightOutside() && this.spawner.getCurrentInside() > 0) {
            this.spawner.releaseAll(serverLevel, this.blockPosition(), null);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        this.spawner.toNBT(valueOutput);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        this.spawner.fromNBT(valueInput);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) { return false; }
}
