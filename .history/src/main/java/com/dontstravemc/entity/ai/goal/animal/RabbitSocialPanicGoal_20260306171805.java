package com.dontstravemc.entity.ai.goal.animal;

import com.dontstravemc.entity.animal.RabbitEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;
import java.util.List;

public class RabbitSocialPanicGoal extends Goal {
    private final RabbitEntity rabbit;
    private int panicTicks = 0;

    public RabbitSocialPanicGoal(RabbitEntity rabbit) {
        this.rabbit = rabbit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.rabbit.getLastHurtByMob() != null) return true;
        
        // Social alarm: check nearby rabbits
        List<RabbitEntity> neighbors = this.rabbit.level().getEntitiesOfClass(
                RabbitEntity.class, 
                this.rabbit.getBoundingBox().inflate(16.0D),
                r -> r != this.rabbit && (r.getLastHurtByMob() != null || r.isAlert())
        );
        
        return !neighbors.isEmpty();
    }

    @Override
    public void start() {
        this.panicTicks = 100; // 5 seconds of panic
    }

    @Override
    public void tick() {
        this.panicTicks--;
        if (this.rabbit.hasHome()) {
            // Force ReturnToDen behavior via logic in RabbitEntity (shouldReturnHome)
            // Or just Navigate here
            com.dontstravemc.entity.component.HomeTetherComponent tether = this.rabbit.getTether();
            net.minecraft.core.BlockPos home = tether.getHomePos();
            this.rabbit.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.5D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.panicTicks > 0;
    }
}
