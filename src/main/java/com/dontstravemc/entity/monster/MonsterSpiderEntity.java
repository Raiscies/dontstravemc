package com.dontstravemc.entity.monster;

import com.dontstravemc.damagesource.ModDamageTypes;
import com.dontstravemc.entity.ai.goal.target.ReturnToDenGoal;
import com.dontstravemc.entity.component.HomeTetherComponent;
import com.dontstravemc.entity.component.SanityAura;
import com.dontstravemc.entity.component.SanityAuraProvider;
import com.dontstravemc.entity.util.stateutil.Eater;
import com.dontstravemc.entity.util.stateutil.Sleepable;
import com.dontstravemc.entity.ai.goal.GenericSleepGoal;
import com.dontstravemc.entity.ai.goal.attack.MonsterSpiderAttackGoal;
import com.dontstravemc.entity.ai.goal.target.EatItemGoal;
import com.dontstravemc.entity.ai.sensing.DynamicSensing;
import com.dontstravemc.entity.util.stateutil.Tetherable;
import com.dontstravemc.event.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;

public class MonsterSpiderEntity extends Monster implements Sleepable, GeoEntity, Eater, Tetherable, SanityAuraProvider<MonsterSpiderEntity> {
    private static final EntityDataAccessor<Boolean> IS_SLEEPING = SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ALERT = SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_ALERTED_TARGET = SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING = SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private final HomeTetherComponent tether = new HomeTetherComponent(8, 24, 32);
    private final SanityAura<MonsterSpiderEntity> sanityAura;
    private LivingEntity pendingTarget;

    public boolean isEating() {
        return this.entityData.get(IS_EATING);
    }

    public boolean isAttacking() {
        return this.entityData.get(IS_ATTACKING);
    }

    public void setAttacking(boolean attacking) {
        this.entityData.set(IS_ATTACKING, attacking);
    }



    private int alertTicks = 0;
    private int attackAnimationTick = 0;
    private int ticksUntilImpact = -1;
    private static final int ATTACK_IMPACT_DELAY = 10;
    private static final EntityDataAccessor<ItemStack> EATING_ITEM =
            SynchedEntityData.defineId(MonsterSpiderEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("spider_walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("spider_attack"); // 播放一次
    private static final RawAnimation ALERT_ANIM = RawAnimation.begin().thenPlay("spider_alert"); // 播放一次
    private static final RawAnimation GO_TO_SLEEP_ANIM = RawAnimation.begin().thenPlay("spider_sleep").thenLoop("spider_sleeping");
    private static final RawAnimation WAKE_UP_ANIM = RawAnimation.begin().thenPlay("spider_awake");
    private static final RawAnimation DEAD_ANIM = RawAnimation.begin().thenPlayAndHold("spider_dead");


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public int getAttackDuration() {

        return 45;
    }


    public MonsterSpiderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.sanityAura = new SanityAura<>(this);
        // -25 per minute = -25 / 60 per second
        this.sanityAura.setAura(-25.0 / 60.0);
    }

    public static AttributeSupplier.Builder createSpiderAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

        AnimationController<MonsterSpiderEntity> baseController = new AnimationController<>("Base", 5, state -> {

            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEAD_ANIM);
            }

            if (this.isAttacking()) {
                state.setControllerSpeed(1.0F);
                return state.setAndContinue(ATTACK_ANIM);
            }

            if (this.isEating()) {
                state.setControllerSpeed(0.8F); // 进食动作慢一点
                return state.setAndContinue(ATTACK_ANIM);
            }


            if (this.isAsleep()) {
                return state.setAndContinue(GO_TO_SLEEP_ANIM);
            }

            if (state.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 0.0001) {
                state.setControllerSpeed(2.0F);
                return state.setAndContinue(WALK_ANIM);
            }

            // 优先级 5: 警戒 (仅在不移动且不睡觉时)
            if (this.isAlert()) {
                return state.setAndContinue(ALERT_ANIM);
            }

            // 默认：停止或闲置
            return PlayState.STOP;
        });

        // 2. 【核心修改】在这里设置声音处理器
        baseController.setSoundKeyframeHandler(event -> {
            String soundName = event.keyframeData().getSound();

            // 播放声音逻辑
            this.level().playLocalSound(
                    this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dontstravemc", soundName)
                    ),
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    1.0F,
                    1.0F,
                    false
            );

        });

        // 3. 将配置好的控制器添加进去
        controllers.add(baseController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // --- 同步数据访问器 ---
    public boolean isAsleep() {
        return this.entityData.get(IS_SLEEPING);
    }

    public void setAsleep(boolean asleep) {
        this.entityData.set(IS_SLEEPING, asleep);
    }

    public boolean isAlert() {
        return this.entityData.get(IS_ALERT);
    }

    public void setAlert(boolean alert) {
        this.entityData.set(IS_ALERT, alert);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MonsterSpiderAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new ReturnToDenGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new EatItemGoal(this, 1.0D, stack -> stack.has(DataComponents.FOOD)));
        this.goalSelector.addGoal(4, new GenericSleepGoal(this, mob -> {
            boolean isDayTime = mob.level().isBrightOutside();
            boolean hasNoHome = !((Tetherable)mob).hasHome();
            return isDayTime && hasNoHome;
        }));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.animal.IronGolem.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // 仅在服务器端处理逻辑
        if (!this.level().isClientSide()) {
            long dayTime = this.level().getDayTime() % 24000;
            boolean isDay = dayTime < 12000 || dayTime > 23000;

            DynamicSensing.updateFollowRangeByTime(this, 0.5D, 1.5D);
            LivingEntity target = this.getTarget();

            // 3. 修改保底回巢逻辑：使用组件获取坐标
            if (isDay && target == null && this.hasHome()) {
                BlockPos home = this.tether.getHomePos(); // 改为从组件获取
                if (home != null) {
                    double distSq = this.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                    if (distSq < 4.0D) {
                        this.forceEnterDen(home);
                        return;
                    }
                }
            }
            // =============================================================

            if (target != null && target.isAlive()) {
                // 3. 唤醒逻辑：如果有目标且正在睡觉，直接唤醒
                if (this.isAsleep()) {
                    this.setAsleep(false);
                }

                // 4. Alert 警觉逻辑
                if (!this.entityData.get(HAS_ALERTED_TARGET)) {
                    LivingEntity currentTarget = this.getTarget();
                    if (currentTarget != null && this.distanceTo(currentTarget) < 8.0D) {
                        if (currentTarget instanceof Player || currentTarget instanceof net.minecraft.world.entity.animal.IronGolem) {
                            this.setAlert(true);
                            this.entityData.set(HAS_ALERTED_TARGET, true);
                            this.alertTicks = 30;
                        }
                    }
                }
            } else {
                // 5. 重置 Alert 标记
                if (this.entityData.get(HAS_ALERTED_TARGET)) {
                    this.entityData.set(HAS_ALERTED_TARGET, false);
                    this.setAlert(false);
                }

                // --- 自动睡觉补充：如果是白天、没目标、且没有巢穴的蜘蛛，AI会自动执行SleepGoal ---
                // 这里不需要写代码，AI Goal 系统会自动处理无巢蜘蛛的睡觉
            }

            // 计时器逻辑递减
            if (this.alertTicks > 0) this.alertTicks--;
            if (this.alertTicks <= 0) this.setAlert(false);

            if (this.ticksUntilImpact > 0) {
                this.ticksUntilImpact--;
                if (this.ticksUntilImpact == 0) this.performActualImpact();
            }

            if (this.attackAnimationTick > 0) {
                this.attackAnimationTick--;
                this.setAttacking(true);
            } else {
                this.setAttacking(false);
            }
        }

        // 客户端粒子效果逻辑 (保持不变)
        if (this.level().isClientSide()) {
            if (this.isEating()) {
                ItemStack eatingStack = this.getEatingItem();
                if (!eatingStack.isEmpty()) {
                    double px = this.getX() + this.getLookAngle().x * 0.5;
                    double py = this.getY() + 0.2;
                    double pz = this.getZ() + this.getLookAngle().z * 0.5;
                    this.level().addParticle(
                            new net.minecraft.core.particles.ItemParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.ITEM, eatingStack),
                            px, py, pz, (this.random.nextFloat() - 0.5) * 0.1, 0.1, (this.random.nextFloat() - 0.5) * 0.1
                    );
                }
            }
        }
    }





    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null && this.isAsleep()) {
            this.setAsleep(false);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_SLEEPING, false);
        builder.define(IS_ALERT, false);
        builder.define(HAS_ALERTED_TARGET, false);
        builder.define(IS_ATTACKING, false);
        builder.define(IS_EATING, false);
        builder.define(EATING_ITEM, ItemStack.EMPTY);
    }


    public void startAttackAnimation(LivingEntity target) {
        this.attackAnimationTick = 25; // 整个动作的时长
        this.ticksUntilImpact = ATTACK_IMPACT_DELAY; // 伤害判定的倒计时
        this.pendingTarget = target;

        this.setAttacking(true);
        this.swing(InteractionHand.MAIN_HAND);
    }

    private void performActualImpact() {
        if (this.pendingTarget != null && this.pendingTarget.isAlive() && this.distanceToSqr(this.pendingTarget) < 4.0D) {
            if (this.level() instanceof ServerLevel serverLevel) {
                // 这里才真正调用原版的伤害方法
                super.doHurtTarget(serverLevel, this.pendingTarget);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel world, Entity target) {
        // 如果不是通过我们自己的计时器触发的，就拦截并转交给延迟逻辑
        if (this.ticksUntilImpact <= 0 && target instanceof LivingEntity living) {
            this.startAttackAnimation(living);
            return false;
        }
        return super.doHurtTarget(world, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 1. 执行原版的受击逻辑（扣血等）
        boolean hasBeenHurt = super.hurtServer(level, source, amount);

        // 2. 如果攻击者是生物（玩家、铁傀儡等），则发起求救
        if (hasBeenHurt && source.getEntity() instanceof LivingEntity attacker) {
            this.broadcastCallForHelp(level, attacker);
            
            // 3. 检查蜘蛛是否在粘性蛛网上被攻击，如果是则通知巢穴派出增援
            BlockPos standingPos = this.blockPosition();
            if (level.getBlockState(standingPos).getBlock() instanceof com.dontstravemc.block.custom.StickyWebbingBlock) {
                // 寻找最近的蜘蛛巢并请求增援
                List<SpiderDenEntity> dens = level.getEntitiesOfClass(
                    SpiderDenEntity.class,
                    this.getBoundingBox().inflate(16.0D),
                    den -> den.isAlive()
                );
                
                if (!dens.isEmpty()) {
                    // 按距离排序，通知最近的巢穴
                    dens.sort(Comparator.comparingDouble(d -> d.distanceToSqr(this.position())));
                    SpiderDenEntity nearestDen = dens.get(0);
                    nearestDen.onSpiderAttackedOnWeb(attacker);
                }
            }
        }

        return hasBeenHurt;
    }

    /**
     * 核心逻辑：向周围同类发送求救广播
     */
    private void broadcastCallForHelp(ServerLevel level, LivingEntity attacker) {
        // 寻找周围 32 格内的所有 MonsterSpiderEntity
        List<MonsterSpiderEntity> neighbors = level.getEntitiesOfClass(
                MonsterSpiderEntity.class,
                this.getBoundingBox().inflate(32.0D),
                spider -> spider != this // 排除掉被打的自己
        );

        for (MonsterSpiderEntity neighbor : neighbors) {
            // 如果邻居目前没有目标，或者邻居正在睡觉
            if (neighbor.getTarget() == null || neighbor.isAsleep()) {
                neighbor.setTarget(attacker); // 强制索敌攻击者
                neighbor.setAsleep(false);   // 如果在睡觉，立即惊醒

                // 可选：触发邻居的警觉动作，让它们整齐划一地看向敌人
                neighbor.setAlert(true);
            }
        }
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        if (source.is(ModDamageTypes.EATEN_MONSTER_FOOD)) {
            return null;
        }

        return ModSounds.MONSTER_SPIDER_HURT;
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        // 顺便把死亡音效也接上
        return ModSounds.MONSTER_SPIDER_DEATH;
    }


    public ItemStack getEatingItem() {
        return this.entityData.get(EATING_ITEM);
    }

    @Override
    public void setEating(boolean eating) {

        this.entityData.set(IS_EATING, eating);
    }

    @Override
    public void setEatingItem(ItemStack stack) {

        this.entityData.set(EATING_ITEM, stack);
    }

    @Override
    public SoundEvent getEatingSound() {
        return ModSounds.MONSTER_SPIDER_EAT;
    }

    @Override
    public int getEatingDuration() {
        // 如果你想让蜘蛛吃得很快（0.5秒），就设为 10
        // 如果想让它吃得慢（3秒），就设为 60
        return 12;
    }

    @Override
    public int getPreEatingDuration() {
        // 设置为 8 或 10 帧。
        // 这意味着蜘蛛看到肉，开始播放动画（低头/扑咬）。
        // 在这 10 帧里，玩家还能看到肉在地上。
        // 第 11 帧，肉消失，蜘蛛进入循环咀嚼声音和粒子阶段。
        return 8;
    }

    @Override
    public int getEatingSoundInterval() {
        // 数字越小，咀嚼声越密集（咔咔咔咔咔）
        // 数字越大，咀嚼声越缓慢（咔...咔...咔）
        return 10;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        super.addAdditionalSaveData(valueOutput);
        // 删除原来的 ValueOutput.store("HomeDenPos"...)
        this.tether.toNBT(valueOutput);
        this.sanityAura.toNBT(valueOutput);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        super.readAdditionalSaveData(valueInput);
        // 删除原来的 ValueInput.read("HomeDenPos"...)
        this.tether.fromNBT(valueInput);
        this.sanityAura.fromNBT(valueInput);
    }

    @Override
    public HomeTetherComponent getTether() { return this.tether; }

    @Override
    public void setHomePos(BlockPos pos) { this.tether.setHomePos(pos); }

    @Override
    public boolean hasHome() {
        return this.tether.getHomePos() != null;
    }

    @Override
    public boolean shouldReturnHome() {
        if (this.getTarget() != null) return false;
        if (this.isEating()) return false;

        long time = this.level().getDayTime() % 24000;
        return time < 12000 || time > 23000;
    }

    private void forceEnterDen(BlockPos pos) {
        this.level().getEntitiesOfClass(SpiderDenEntity.class, new AABB(pos).inflate(4.0D))
                .stream().findFirst().ifPresent(den -> {
                    den.occupantReturned(this);
                    this.discard();
                });
    }

    public SanityAura<MonsterSpiderEntity> getSanityAura() {
        return this.sanityAura;
    }
    
    /**
     * 检查物品是否是蜘蛛的食物（用于陷阱诱饵判定）
     * 蜘蛛吃所有食物
     */
    public boolean isFood(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }
}
