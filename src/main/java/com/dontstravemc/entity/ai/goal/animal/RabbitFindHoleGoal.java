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
        // 无家的兔子不应该主动寻找洞穴
        // 它们应该在世界中自由游荡，只有从洞穴生成的兔子才有家
        return false;
    }

    @Override
    public void start() {
        // 不再执行
    }
}
