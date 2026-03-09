package com.dontstravemc.entity.ai.goal.animal;

import com.dontstravemc.entity.animal.RabbitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 让兔子在白天没有其他目标时在兔子洞周围散步
 */
public class RabbitStrollAroundHomeGoal extends Goal {
    private final RabbitEntity rabbit;
    private final double speed;
    private final int maxDistance;

    public RabbitStrollAroundHomeGoal(RabbitEntity rabbit, double speed, int maxDistance) {
        this.rabbit = rabbit;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // 只有在白天、有家、不需要回家、且不在警惕状态时才散步
        if (!this.rabbit.hasHome()) return false;
        if (this.rabbit.shouldReturnHome()) return false;
        if (this.rabbit.isAlert()) return false;
        if (this.rabbit.getLastHurtByMob() != null) return false;
        
        // 每隔一段时间才尝试散步（避免过于频繁）
        return this.rabbit.getRandom().nextInt(120) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        // 如果开始警惕或需要回家，停止散步
        return !this.rabbit.getNavigation().isDone() 
                && !this.rabbit.isAlert() 
                && !this.rabbit.shouldReturnHome()
                && this.rabbit.getLastHurtByMob() == null;
    }

    @Override
    public void start() {
        Vec3 targetPos = this.findStrollPosition();
        if (targetPos != null) {
            this.rabbit.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speed);
        }
    }

    private Vec3 findStrollPosition() {
        BlockPos homePos = this.rabbit.getTether().getHomePos();
        if (homePos == null) return null;

        // 在兔子洞周围寻找随机位置
        Vec3 homeVec = new Vec3(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5);
        
        // 尝试在兔子洞周围找一个合适的位置
        for (int i = 0; i < 10; i++) {
            double angle = this.rabbit.getRandom().nextDouble() * Math.PI * 2;
            double distance = this.rabbit.getRandom().nextDouble() * this.maxDistance;
            
            double x = homeVec.x + Math.cos(angle) * distance;
            double z = homeVec.z + Math.sin(angle) * distance;
            
            Vec3 targetPos = new Vec3(x, homeVec.y, z);
            
            // 检查目标位置是否可达
            if (this.rabbit.getNavigation().isStableDestination(BlockPos.containing(targetPos))) {
                return targetPos;
            }
        }
        
        // 如果上面的方法失败，使用默认的随机位置生成
        Vec3 randomPos = DefaultRandomPos.getPos(this.rabbit, this.maxDistance, 3);
        return randomPos;
    }

    @Override
    public void stop() {
        this.rabbit.getNavigation().stop();
    }
}
