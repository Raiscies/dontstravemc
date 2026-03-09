package com.dontstravemc.entity.animal;

import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.component.DenSpawnerComponent;
import com.dontstravemc.entity.util.stateutil.Den;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
        // 设置实体为无敌
        this.setInvulnerable(true);
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 10.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.0D);
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

        // Release at daybreak only if no players are nearby
        if (serverLevel.isBrightOutside() && this.spawner.getCurrentInside() > 0) {
            // 检查10格内是否有玩家
            boolean hasNearbyPlayer = serverLevel.getNearestPlayer(
                this.getX(), this.getY(), this.getZ(), 
                10.0D, 
                false
            ) != null;
            
            // 只有在没有玩家在附近时才释放兔子
            if (!hasNearbyPlayer) {
                this.spawner.releaseAll(serverLevel, this.blockPosition(), null);
            }
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
    public boolean isPushable() { 
        return false; 
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) { 
        return false; 
    }
}
