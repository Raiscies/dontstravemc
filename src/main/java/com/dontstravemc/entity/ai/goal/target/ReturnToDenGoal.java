package com.dontstravemc.entity.ai.goal.target;

import com.dontstravemc.block.entity.RabbitHoleBlockEntity;
import com.dontstravemc.entity.component.HomeTetherComponent;
import com.dontstravemc.entity.util.stateutil.Den;
import com.dontstravemc.entity.util.stateutil.Tetherable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 回家寻路目标 - 让生物寻路回到自己的巢穴
 * 支持两种类型的巢穴：
 * 1. 方块实体（BlockEntity）- 如 RabbitHoleBlockEntity
 * 2. 实体（Entity）- 如 SpiderDenEntity
 */
public class ReturnToDenGoal extends Goal {
    private final PathfinderMob mob;
    private final Tetherable tetherable;
    private final double speed;

    public ReturnToDenGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.tetherable = (Tetherable) mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // 1. 只有"有巢穴"的生物才使用这个回家 AI
        if (!tetherable.hasHome() || tetherable.getTether().getHomePos() == null) return false;

        // 2. 只有在回家时间（夜晚）且没有战斗目标时触发
        return tetherable.shouldReturnHome() && mob.getTarget() == null;
    }

    @Override
    public void tick() {
        BlockPos home = tetherable.getTether().getHomePos();
        if (home == null) return;

        double distSq = mob.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);

        // 如果距离中心点小于 1.5 格，尝试进入巢穴
        if (distSq < 2.25D) {
            // 优先检查是否是方块实体（如兔子洞）
            BlockEntity blockEntity = mob.level().getBlockEntity(home);
            if (blockEntity instanceof RabbitHoleBlockEntity rabbitHoleBlockEntity) {
                // 方块实体类型的巢穴，由 RabbitEnterHoleGoal 处理进入逻辑
                // 这里不做任何操作，让专门的进入目标处理
                return;
            }
            
            // 如果不是方块实体，则检查是否是实体类型的巢穴（如蜘蛛巢）
            this.enterDenEntity();
        } else {
            // 只要还没到中心，就一直强制寻路，防止"忘记回家"
            if (mob.getNavigation().isDone() || mob.tickCount % 20 == 0) {
                mob.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, this.speed);
            }
        }
    }

    /**
     * 进入实体类型的巢穴（如蜘蛛巢）
     */
    private void enterDenEntity() {
        BlockPos home = tetherable.getTether().getHomePos();
        if (home == null) return;
        
        // 在家的位置搜索洞穴实体，扩大搜索范围到5格
        this.mob.level().getEntitiesOfClass(Mob.class,
                        this.mob.getBoundingBox().inflate(5.0D),
                        entity -> entity instanceof Den && entity.blockPosition().equals(home))
                .stream().findFirst().ifPresent(denEntity -> {
                    ((Den) denEntity).occupantReturned(this.mob);
                    this.mob.discard(); // 消失，进入巢穴
                });
    }
}
