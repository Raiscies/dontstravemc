package com.dontstravemc.entity.monster;

import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.component.DenSpawnerComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.dontstravemc.entity.util.WebbingOwnershipManager;

import java.util.UUID;

public class SpiderDenEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean hasGeneratedWebbing = false;
    private static final String NBT_KEY_GENERATED_WEBBING = "HasGeneratedWebbing";
    private final DenSpawnerComponent spawner;

    public SpiderDenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);


        this.spawner = new DenSpawnerComponent(() -> ModEntities.MONSTER_SPIDER, 3, 90);
    }

    // --- 1.21.x 属性设置 ---
    public static AttributeSupplier.Builder createSpiderDenAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // --- 核心修复：1.21.x ValueOutput 架构 ---
    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        valueOutput.putBoolean(NBT_KEY_GENERATED_WEBBING, this.hasGeneratedWebbing);
        this.spawner.toNBT(valueOutput);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        this.hasGeneratedWebbing = valueInput.getBooleanOr(NBT_KEY_GENERATED_WEBBING, false);
    }

    public void occupantReturned(Mob mob) { // 将参数从 MonsterSpiderEntity 改为 Mob
        if (!this.level().isClientSide()) {
            // 组件内部使用的是 UUID，所以传什么 Mob 进来都可以
            this.spawner.occupantReturned(mob.getUUID());
        }
    }

    // --- 核心修复：绕过不稳定的加载方法，改用 tick 触发 ---
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level();

        // 只在服务端运行，且只在生成后的第一个 tick 运行一次
        if (!this.level().isClientSide() && !this.hasGeneratedWebbing) {
            this.generateStickyWebbing();
            this.hasGeneratedWebbing = true;
        }

        if (!serverLevel.isBrightOutside() && this.spawner.getCurrentInside() > 0) {
            this.spawner.releaseAll(serverLevel, this.blockPosition(), null);
        }
    }

    private void generateStickyWebbing() {
    int radius = 5;
    BlockPos center = this.blockPosition();
    double radiusSq = radius * radius;
    
    // Store UUID for ownership tracking
    UUID denUUID = this.getUUID();
    WebbingOwnershipManager manager = WebbingOwnershipManager.getInstance();
    
    BlockPos.betweenClosedStream(
            center.offset(-radius, -1, -radius),
            center.offset(radius, 1, radius)
    ).forEach(pos -> {
        // Calculate horizontal distance squared
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double distanceSq = dx * dx + dz * dz;
        
        // Only execute within circular range
        if (distanceSq <= radiusSq) {
            BlockState currentState = this.level().getBlockState(pos);
            
            // Case 1: Position already has webbing from another den
            if (currentState.is(ModBlocks.STICKY_WEBBING)) {
                // Just add this den as an owner, don't replace the block
                manager.addOwner(pos, denUUID);
            }
            // Case 2: Position is empty, try to create new webbing
            else if (this.level().isEmptyBlock(pos)) {
                BlockState webbingState = ModBlocks.STICKY_WEBBING.defaultBlockState();
                boolean hasAnyFace = false;
                
                // Check all 6 directions for solid adjacent blocks
                for (Direction direction : Direction.values()) {
                    // Skip UP face (underside of blocks above) - optional
                    if (direction == Direction.UP) continue;
                    
                    BlockPos adjacentPos = pos.relative(direction);
                    BlockState adjacentState = this.level().getBlockState(adjacentPos);
                    
                    // Check if adjacent block has a sturdy face pointing toward our position
                    if (adjacentState.isFaceSturdy(this.level(), adjacentPos, direction.getOpposite())) {
                        webbingState = webbingState.setValue(
                            MultifaceBlock.getFaceProperty(direction), 
                            true
                        );
                        hasAnyFace = true;
                    }
                }
                
                // Only place the webbing if at least one face was found
                if (hasAnyFace) {
                    this.level().setBlock(pos, webbingState, 3);
                    // Register ownership
                    manager.addOwner(pos, denUUID);
                }
            }
        }
    });
}

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide()) {
            this.removeNearbyWebbing();
        }
    }

    private void removeNearbyWebbing() {
        int radius = 5;
        BlockPos center = this.blockPosition();
        double radiusSq = radius * radius;
        
        UUID denUUID = this.getUUID();
        WebbingOwnershipManager manager = WebbingOwnershipManager.getInstance();

        BlockPos.betweenClosedStream(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius)
        ).forEach(pos -> {
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            double distanceSq = dx * dx + dz * dz;

            if (distanceSq <= radiusSq) {
                if (this.level().getBlockState(pos).is(ModBlocks.STICKY_WEBBING)) {
                    // Only remove if no other dens own this webbing
                    if (!manager.hasOtherOwners(pos, denUUID)) {
                        this.level().removeBlock(pos, false);
                        manager.clearPosition(pos);
                    } else {
                        // Just remove this den from ownership, keep the block
                        manager.removeOwner(pos, denUUID);
                    }
                }
            }
        });
    }

    public void onWebTriggered(BlockPos webPos, LivingEntity intruder) {
        if (this.level().isClientSide()) return;

        // 只有当外面没有任何蜘蛛（ActiveCount == 0）且库存有货时，才释放一只守卫
        if (this.spawner.getActiveCount() == 0 && this.spawner.getCurrentInside() > 0) {
            Mob guard = this.spawner.releaseOne((ServerLevel)this.level(), this.blockPosition(), intruder);
            if (guard != null) {
                // 让它冲向被踩的网
                guard.getNavigation().moveTo(webPos.getX(), webPos.getY(), webPos.getZ(), 1.25D);
            }
        }
    }

    /**
     * Called when a spider is attacked while on StickyWebbing.
     * Spawns 2 spiders as reinforcement to defend.
     */
    public void onSpiderAttackedOnWeb(LivingEntity attacker) {
        if (this.level().isClientSide()) return;

        // Spawn 2 spiders from the den to reinforce
        ServerLevel serverLevel = (ServerLevel) this.level();
        for (int i = 0; i < 2 && this.spawner.getCurrentInside() > 0; i++) {
            Mob reinforcement = this.spawner.releaseOne(serverLevel, this.blockPosition(), attacker);
            if (reinforcement != null && attacker != null) {
                // Make the reinforcement target the attacker
                reinforcement.setTarget(attacker);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean result = super.hurtServer(level, source, amount);
        if (result && source.getEntity() instanceof LivingEntity attacker) {
            // 释放所有剩余库存攻击攻击者
            this.spawner.releaseAll(level, this.blockPosition(), attacker);
        }
        return result;
    }

    // --- GeckoLib 接口实现 ---
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- 锁定逻辑 ---
    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) {
        return false;
    }

}