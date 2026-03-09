package com.dontstravemc.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ButterflyEntity extends PathfinderMob {

    private float waveOffset;

    public final AnimationState flyAnimationState = new AnimationState();
    public final AnimationState stayAnimationState = new AnimationState();
    public final AnimationState dieAnimationState = new AnimationState();

    public static final int FLOWER_COOLDOWN_TICKS = 600;
    private int flowerCooldown = 0;

    private static final EntityDataAccessor<Boolean> DATA_IS_RESTING =
            SynchedEntityData.defineId(ButterflyEntity.class, EntityDataSerializers.BOOLEAN);

    public ButterflyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.waveOffset = this.random.nextFloat() * (float) Math.PI * 2f;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setResting(false);

        if (level.isClientSide()) {
            this.flyAnimationState.start(this.tickCount);
        }
    }

    /* ---------------- 基础状态 ---------------- */

    public boolean canSearchFlower() {
        return flowerCooldown <= 0;
    }

    public void setFlowerCooldown(int ticks) {
        this.flowerCooldown = ticks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IS_RESTING, false);
    }

    public void setResting(boolean resting) {
        this.entityData.set(DATA_IS_RESTING, resting);
    }

    public boolean isResting() {
        return this.entityData.get(DATA_IS_RESTING);
    }

    /* ---------------- AI ---------------- */

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ButterflyAvoidPlayerGoal(this));
        this.goalSelector.addGoal(2, new ButterflyRestOnFlowerGoal(this));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                int groundY = ButterflyEntity.this.level().getHeight(
                        Heightmap.Types.MOTION_BLOCKING,
                        ButterflyEntity.this.getBlockX(),
                        ButterflyEntity.this.getBlockZ()
                );
                if (ButterflyEntity.this.getY() > groundY + 5) return false;
                return super.canUse();
            }
        });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    /* ---------------- Tick ---------------- */

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && flowerCooldown > 0) {
            flowerCooldown--;
        }

        if (!this.level().isClientSide() && this.isAlive()) {

            double horizontalSpeed = this.getDeltaMovement().horizontalDistance();
            if (horizontalSpeed > 0.01) {
                double wave = Math.sin((this.tickCount * 0.2f) + this.waveOffset) * 0.01f;
                this.setDeltaMovement(this.getDeltaMovement().add(0, wave, 0));
            }

            int groundY = this.level().getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    this.getBlockX(),
                    this.getBlockZ()
            );

            if (this.getY() > groundY + 5) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05, 0));
            }
        }

        if (this.level().isClientSide()) {
            if (!this.isAlive()) {
                dieAnimationState.startIfStopped(this.tickCount);
                flyAnimationState.stop();
                stayAnimationState.stop();
            } else if (this.isResting()) {
                stayAnimationState.startIfStopped(this.tickCount);
            } else {
                flyAnimationState.startIfStopped(this.tickCount);
            }
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    protected double getDefaultGravity() {
        return 0.02;
    }

    @Override
    public void travel(Vec3 movementInput) {
        if (!this.onGround()) {
            Vec3 v = this.getDeltaMovement();
            if (v.y < -0.05) {
                this.setDeltaMovement(v.x, -0.05, v.z);
            }
        }
        super.travel(movementInput);
    }

    /* ---------------- 受惊逃跑 ---------------- */

    class ButterflyAvoidPlayerGoal extends Goal {
        private final ButterflyEntity butterfly;
        private Player targetPlayer;

        private static final double TRIGGER_DISTANCE = 4.0;
        private static final double ESCAPE_SPEED = 1.3;
        private static final int CHECK_CHANCE = 4;

        ButterflyAvoidPlayerGoal(ButterflyEntity butterfly) {
            this.butterfly = butterfly;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (butterfly.getRandom().nextInt(CHECK_CHANCE) != 0) return false;

            targetPlayer = butterfly.level().getNearestPlayer(butterfly, TRIGGER_DISTANCE);

            // 核心修改：如果玩家为空，或者玩家正在潜行，则不触发惊吓
            if (targetPlayer == null || targetPlayer.isShiftKeyDown()) {
                return false;
            }

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return targetPlayer != null
                    && targetPlayer.isAlive()
                    && butterfly.distanceToSqr(targetPlayer) <= TRIGGER_DISTANCE * TRIGGER_DISTANCE;
        }

        @Override
        public void start() {
            butterfly.setResting(false);
        }

        @Override
        public void tick() {
            // 每隔固定频率更新路径，避免每 tick 运算导致的卡顿
            if (butterfly.getRandom().nextInt(5) == 0) {
                updateEscape();
            }
        }

        private void updateEscape() {
            Vec3 butterflyPos = butterfly.position();
            Vec3 playerPos = targetPlayer.position();

            // 计算逃跑向量：远离玩家
            Vec3 escapeDir = butterflyPos.subtract(playerPos).normalize();

            // 饥荒蝴蝶逃跑时通常会稍微拉升高度
            // 目标点设为远离玩家 6 格，且 Y 轴稍微抬高 1-2 格，防止撞地
            Vec3 target = butterflyPos.add(escapeDir.x * 6.0, 1.5, escapeDir.z * 6.0);

            butterfly.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    ESCAPE_SPEED
            );
        }
    }


    class ButterflyRestOnFlowerGoal extends Goal {

        private final ButterflyEntity butterfly;
        private BlockPos flowerPos;

        private int restTimer = 0;
        private int maxRestTicks;

        ButterflyRestOnFlowerGoal(ButterflyEntity butterfly) {
            this.butterfly = butterfly;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!butterfly.canSearchFlower()) return false;
            if (butterfly.getRandom().nextInt(10) != 0) return false;

            BlockPos center = butterfly.blockPosition();
            List<BlockPos> flowers = new ArrayList<>();

            for (BlockPos pos : BlockPos.betweenClosed(
                    center.offset(-8, -4, -8),
                    center.offset(8, 4, 8))) {

                if (butterfly.level().getBlockState(pos).getBlock() instanceof FlowerBlock) {
                    flowers.add(pos.immutable());
                }
            }

            if (flowers.isEmpty()) return false;
            flowerPos = flowers.get(butterfly.getRandom().nextInt(flowers.size()));
            return true;
        }

        @Override
        public void start() {
            restTimer = 0;
            Vec3 target = Vec3.atCenterOf(flowerPos).add(0, 0.5, 0);
            butterfly.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }

        @Override
        public void tick() {
            // 修正：使用 position() 和 Vec3
            double dist = butterfly.position().distanceTo(Vec3.atBottomCenterOf(flowerPos));

            // 只有当距离花朵足够近时，才开始“停歇计数”
            if (dist < 0.6) { // 稍微放大判定范围，防止因为重力抖动无法触发
                butterfly.setResting(true);
                butterfly.getNavigation().stop();
                restTimer++;
            } else {
                // 如果还在飞行途中，确保不是 Resting 状态
                butterfly.setResting(false);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return restTimer < maxRestTicks;
        }

        @Override
        public void stop() {
            butterfly.setResting(false);
            butterfly.setFlowerCooldown(FLOWER_COOLDOWN_TICKS);
            flowerPos = null;
        }
    }
}
