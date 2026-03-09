package com.dontstravemc.entity.ai.goal;

import com.dontstravemc.entity.util.stateutil.Sleepable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.function.Predicate;

public class GenericSleepGoal extends Goal {
    protected final Mob mob;
    protected final Sleepable sleepable;
    protected final Predicate<Mob> sleepCondition; // 睡眠触发条件

    /**
     * @param mob 绑定的实体
     * @param sleepCondition 传入一个 Lambda 表达式或方法引用作为判定条件
     */
    public GenericSleepGoal(Mob mob, Predicate<Mob> sleepCondition) {
        this.mob = mob;
        this.sleepCondition = sleepCondition;
        if (mob instanceof Sleepable s) {
            this.sleepable = s;
        } else {
            throw new IllegalArgumentException("Entity must implement Sleepable interface");
        }
        // 锁定移动、转头和跳跃
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // 基本硬性条件：没有目标且不在水里
        if (this.mob.getTarget() != null || this.mob.getLastHurtByMob() != null) {
            return false;
        }
        // 关键修正 2：如果当前不需要睡觉（比如晚上），直接返回 false，释放 Flag
        return this.sleepCondition.test(this.mob);
    }

    @Override
    public void start() {
        this.sleepable.setAsleep(true);
        this.mob.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.sleepable.setAsleep(false);
    }

    @Override
    public boolean canContinueToUse() {
        // 如果被打、有了目标、或者条件不再满足，就醒来
        if (this.mob.getTarget() != null || this.mob.getLastHurtByMob() != null) {
            return false;
        }

        // 2. 环境判定：如果进水了，或者 sleepCondition (白天) 不再满足，就醒来
        return !this.mob.isInWater() && this.sleepCondition.test(this.mob);
    }
}