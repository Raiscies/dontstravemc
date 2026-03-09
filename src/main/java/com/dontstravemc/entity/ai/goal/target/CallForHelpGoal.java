package com.dontstravemc.entity.ai.goal.target;

import net.minecraft.world.entity.PathfinderMob; // 必须是这个
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import com.dontstravemc.entity.util.stateutil.Sleepable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class CallForHelpGoal extends HurtByTargetGoal {

    public CallForHelpGoal(PathfinderMob mob) {
        super(mob);
        this.setAlertOthers();
    }

    @Override
    protected void alertOther(Mob mob, LivingEntity target) {
        super.alertOther(mob, target);

        // 同步惊醒和警觉逻辑
        if (mob instanceof Sleepable sleepable) {
            sleepable.setAsleep(false);
        }

    }
}