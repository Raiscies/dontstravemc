package com.dontstravemc.entity.ai.goal.target;

import com.dontstravemc.dsfood.custom.HealingFoodItem;
import com.dontstravemc.entity.util.stateutil.Eater;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EatItemGoal extends Goal {
    protected final Mob mob;
    protected final Predicate<ItemStack> foodPredicate;
    protected ItemEntity targetItem;
    protected ItemStack heldStack = ItemStack.EMPTY;
    protected final double speedModifier;

    private int eatingTimer = 0;
    private int lungeTimer = 0; // 预备动作计时器
    private int scanTick = 0;

    public EatItemGoal(Mob mob, double speed, Predicate<ItemStack> foodPredicate) {
        this.mob = mob;
        this.speedModifier = speed;
        this.foodPredicate = foodPredicate;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) return false;
        if (this.heldStack.isEmpty() && this.lungeTimer <= 0) {
            if (this.targetItem == null || !this.targetItem.isAlive()) {
                if (--this.scanTick > 0) return false;
                this.scanTick = 20;
                this.targetItem = com.dontstravemc.entity.ai.sensing.ItemSensing.findNearestFood(this.mob, 8.0D, this.foodPredicate);
            }
        }
        return (this.lungeTimer > 0) || (!this.heldStack.isEmpty()) || (this.targetItem != null && this.targetItem.isAlive());
    }

    @Override
    public void tick() {
        if (this.heldStack.isEmpty() && this.lungeTimer <= 0) {
            // --- 阶段 1：接近阶段 ---
            if (this.targetItem == null || !this.targetItem.isAlive()) return;

            double reachDist = (double)(this.mob.getBbWidth() * 0.5F + 0.8F);
            double distSq = this.mob.distanceToSqr(this.targetItem);

            this.mob.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);

            if (distSq > reachDist * reachDist) {
                this.mob.getNavigation().moveTo(this.targetItem, this.speedModifier);
            }

            if (distSq <= reachDist * reachDist || this.mob.getNavigation().isDone()) {
                // --- 【核心时机】开始做动作，但先不拿走食物 ---
                if (this.mob instanceof Eater eater) {
                    this.lungeTimer = eater.getPreEatingDuration();
                    eater.setEating(true); // 立即触发动画
                    this.mob.getNavigation().stop();
                } else {
                    this.grabItem(); // 不支持延迟的生物直接抓取
                }
            }
        } else if (this.lungeTimer > 0) {
            // --- 阶段 2：预备动作阶段（肉还在地上） ---
            this.lungeTimer--;
            this.mob.getNavigation().stop();

            // 确保一直盯着地上的肉
            if (this.targetItem != null && this.targetItem.isAlive()) {
                this.mob.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);
            }

            // 预备时间结束，真正“咬”到嘴里
            if (this.lungeTimer <= 0) {
                this.grabItem();
            }
        } else {
            // --- 阶段 3：咀嚼阶段（肉已经在嘴里消失了） ---
            this.eatingTimer--;
            this.mob.getNavigation().stop();

            if (this.mob instanceof Eater eater) {
                if (this.eatingTimer % eater.getEatingSoundInterval() == 0) {
                    if (eater.getEatingSound() != null) {
                        this.mob.playSound(eater.getEatingSound(), 1.0F, 0.8F + this.mob.getRandom().nextFloat() * 0.4F);
                    }
                }
            }

            if (this.eatingTimer <= 0) {
                this.finalizeEating();
            }
        }
    }

    private void grabItem() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            this.stop(); // 如果预备动作期间肉被玩家捡了，就结束
            return;
        }

        ItemStack groundStack = this.targetItem.getItem();
        if (!groundStack.isEmpty()) {
            this.heldStack = groundStack.split(1);
            if (groundStack.isEmpty()) {
                this.targetItem.discard();
            }
            this.targetItem = null;

            if (this.mob instanceof Eater eater) {
                this.eatingTimer = eater.getEatingDuration();
                eater.setEatingItem(this.heldStack); // 开始产生粒子
            }
        }
    }

    // finalizeEating 和 stop 逻辑保持之前的一致...
    private void finalizeEating() {
        if (this.heldStack.isEmpty()) return;
        ItemStack resultStack = this.heldStack.finishUsingItem(this.mob.level(), this.mob);
        FoodProperties food = this.heldStack.get(DataComponents.FOOD);
        if (food != null && !(this.heldStack.getItem() instanceof HealingFoodItem)) {
            this.mob.heal(food.nutrition() * 1.5F);
        }
        if (this.mob.level() instanceof ServerLevel serverLevel && !resultStack.isEmpty()) {
            this.mob.spawnAtLocation(serverLevel, resultStack);
        }
        this.heldStack = ItemStack.EMPTY;
        if (this.mob instanceof Eater eater) {
            eater.setEating(false);
            eater.setEatingItem(ItemStack.EMPTY);
        }
    }

    @Override
    public void stop() {
        if (!this.heldStack.isEmpty()) {
            if (this.mob.level() instanceof ServerLevel serverLevel) {
                this.mob.spawnAtLocation(serverLevel, this.heldStack);
            }
        }
        this.heldStack = ItemStack.EMPTY;
        this.eatingTimer = 0;
        this.lungeTimer = 0;
        if (this.mob instanceof Eater eater) {
            eater.setEating(false);
            eater.setEatingItem(ItemStack.EMPTY);
        }
        this.targetItem = null;
    }
}