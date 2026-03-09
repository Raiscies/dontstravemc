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
        double radiusSq = radius * radius; // 预计算半径的平方

        BlockPos.betweenClosedStream(
                center.offset(-radius, -1, -radius),
                center.offset(radius, 1, radius)
        ).forEach(pos -> {
            // 计算水平距离的平方 (忽略 Y 轴高度差，确保生成的是一个圆盘状)
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            double distanceSq = dx * dx + dz * dz;

            // 只有在圆形范围内才执行
            if (distanceSq <= radiusSq) {
                if (this.level().isEmptyBlock(pos) &&
                        this.level().getBlockState(pos.below()).isFaceSturdy(this.level(), pos.below(), Direction.UP)) {

                    BlockState state = ModBlocks.STICKY_WEBBING.defaultBlockState()
                            .setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);

                    this.level().setBlock(pos, state, 3);
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
        int radius = 5; // 移除半径稍微大一点，确保边缘清理干净
        BlockPos center = this.blockPosition();
        double radiusSq = radius * radius;

        BlockPos.betweenClosedStream(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius)
        ).forEach(pos -> {
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            double distanceSq = dx * dx + dz * dz;

            // 只有在圆形范围内才执行
            if (distanceSq <= radiusSq) {
                if (this.level().getBlockState(pos).is(ModBlocks.STICKY_WEBBING)) {
                    this.level().removeBlock(pos, false);
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