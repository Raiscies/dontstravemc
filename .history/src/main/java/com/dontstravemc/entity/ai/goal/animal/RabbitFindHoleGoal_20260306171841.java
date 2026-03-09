package com.dontstravemc.entity.ai.goal.animal;

import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.entity.animal.RabbitHoleEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

public class RabbitFindHoleGoal extends Goal {
    private final RabbitEntity rabbit;

    public RabbitFindHoleGoal(RabbitEntity rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public boolean canUse() {
        return !this.rabbit.hasHome() && this.rabbit.tickCount % 100 == 0;
    }

    @Override
    public void start() {
        List<RabbitHoleEntity> holes = this.rabbit.level().getEntitiesOfClass(
                RabbitHoleEntity.class,
                this.rabbit.getBoundingBox().inflate(32.0D)
        );
        
        if (!holes.isEmpty()) {
            // Bind to the nearest hole
            RabbitHoleEntity nearest = holes.stream()
                    .min((h1, h2) -> Double.compare(this.rabbit.distanceToSqr(h1), this.rabbit.distanceToSqr(h2)))
                    .get();
            this.rabbit.setHomePos(nearest.blockPosition());
        }
    }
}
