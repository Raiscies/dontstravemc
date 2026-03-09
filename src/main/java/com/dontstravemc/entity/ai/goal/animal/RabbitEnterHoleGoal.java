package com.dontstravemc.entity.ai.goal.animal;

import com.dontstravemc.block.entity.RabbitHoleBlockEntity;
import com.dontstravemc.entity.animal.RabbitEntity;
import com.dontstravemc.entity.util.stateutil.Tetherable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 兔子进入洞穴的AI目标 - 参考原版 Bee.BeeEnterHiveGoal
 * 当兔子靠近洞穴时，直接进入洞穴
 */
public class RabbitEnterHoleGoal extends Goal {
    private final RabbitEntity rabbit;
    private final Tetherable tetherable;

    public RabbitEnterHoleGoal(RabbitEntity rabbit) {
        this.rabbit = rabbit;
        this.tetherable = rabbit;
    }

    @Override
    public boolean canUse() {
        // 硬性条件：
        // 1. 有家（洞穴位置）
        // 2. 距离洞穴中心小于2格
        BlockPos homePos = tetherable.getTether().getHomePos();
        if (homePos == null || !homePos.closerToCenterThan(rabbit.position(), 2.0)) {
            return false;
        }
        
        // 检查洞穴方块实体是否存在且未满
        RabbitHoleBlockEntity holeEntity = getRabbitHoleBlockEntity();
        if (holeEntity == null) {
            return false;
        }
        
        if (holeEntity.isFull()) {
            // 如果洞穴已满，清除家的位置
            tetherable.getTether().setHomePos(null);
            return false;
        }
        
        // 软性条件（满足其一即可）：
        // 1. 是夜晚或下雨（正常回家时间）
        // 2. 附近10格内有玩家（被惊吓逃回家）
        boolean isNightOrRaining = tetherable.shouldReturnHome();
        boolean hasNearbyPlayer = rabbit.level().getNearestPlayer(rabbit, 10.0) != null;
        
        return isNightOrRaining || hasNearbyPlayer;
    }

    @Override
    public boolean canContinueToUse() {
        // 这个目标是一次性的，执行后立即结束
        return false;
    }

    @Override
    public void start() {
        // 进入洞穴（参考 BeeEnterHiveGoal.start）
        RabbitHoleBlockEntity holeEntity = getRabbitHoleBlockEntity();
        if (holeEntity != null) {
            holeEntity.addOccupant(rabbit);
        }
    }

    /**
     * 获取兔子洞方块实体
     */
    private RabbitHoleBlockEntity getRabbitHoleBlockEntity() {
        BlockPos homePos = tetherable.getTether().getHomePos();
        if (homePos == null) {
            return null;
        }
        
        BlockEntity blockEntity = rabbit.level().getBlockEntity(homePos);
        if (blockEntity instanceof RabbitHoleBlockEntity rabbitHoleBlockEntity) {
            return rabbitHoleBlockEntity;
        }
        return null;
    }
}
