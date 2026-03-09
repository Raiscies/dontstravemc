package com.dontstravemc.entity.ai.goal.attack;

import com.dontstravemc.entity.component.HomeTetherComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class TetheredAttackGoal extends MeleeAttackGoal {
    private final PathfinderMob pathfinderMob;
    private final HomeTetherComponent tether;

    public TetheredAttackGoal(PathfinderMob mob, HomeTetherComponent tether, double speed, boolean pause) {
        super(mob, speed, pause);
        this.pathfinderMob = mob;
        this.tether = tether;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = pathfinderMob.getTarget();
        BlockPos home = tether.getHomePos();

        if (home != null && target != null) {
            // 目标离开追击范围，放弃攻击
            if (target.distanceToSqr(home.getX(), home.getY(), home.getZ()) > tether.getChaseRadiusSq()) {
                pathfinderMob.setTarget(null);
                return false;
            }
        }
        return super.canContinueToUse();
    }
}
