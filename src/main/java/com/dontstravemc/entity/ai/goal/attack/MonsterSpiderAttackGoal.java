package com.dontstravemc.entity.ai.goal.attack;

import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class MonsterSpiderAttackGoal extends MeleeAttackGoal {
    private final MonsterSpiderEntity spider;
    private int customCooldown = 0;

    public MonsterSpiderAttackGoal(MonsterSpiderEntity spider, double speed, boolean pause) {
        super(spider, speed, pause);
        this.spider = spider;
    }

    @Override
    public void tick() {
        if (this.customCooldown > 0) {
            this.customCooldown--;
        }

        super.tick();

        LivingEntity target = this.spider.getTarget();
        // 这里的逻辑可以保留，用于控制“战斗姿态”
        // 但我们的核心动画现在由 Entity 里的 isAttacking 控制了
        if (target != null && this.customCooldown > 5) {
            this.spider.setAggressive(true);
        } else {
            this.spider.setAggressive(false);
        }
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        // MeleeAttackGoal 的标准距离检查
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();

            // 只要进入攻击尝试，就调用 doHurtTarget
            // 即使对方闪开了或者伤害是0，动画也会播出来
            if (this.spider.level() instanceof ServerLevel serverLevel) {
                this.spider.doHurtTarget(serverLevel, target);
            }
        }
    }

    @Override
    protected boolean isTimeToAttack() {
        // 显式指向我们的计时器
        return this.customCooldown <= 0;
    }

    @Override
    protected void resetAttackCooldown() {
        // 关键 3：重置为实体类定义的时长 (比如 45)
        this.customCooldown = this.adjustedTickDelay(this.spider.getAttackDuration());
    }

    @Override
    public void stop() {
        super.stop();
        this.customCooldown = 0; // 停止追踪时清空冷却
        this.spider.setAggressive(false);
    }
}