package com.dontstravemc.entity.ai.goal.target;

import com.dontstravemc.entity.component.HomeTetherComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob; // 核心修复：使用 PathfinderMob
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TetheredStrollGoal extends WaterAvoidingRandomStrollGoal {
    private final PathfinderMob pathfinderMob; // 修复类型
    private final HomeTetherComponent tether;

    public TetheredStrollGoal(PathfinderMob mob, HomeTetherComponent tether, double speed) {
        super(mob, speed); // 现在 super 能够匹配 (PathfinderMob, double)
        this.pathfinderMob = mob;
        this.tether = tether;
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        BlockPos home = tether.getHomePos();
        if (home == null) return super.getPosition();

        // 检查是否离家太远
        if (pathfinderMob.distanceToSqr(home.getX(), home.getY(), home.getZ()) > tether.getStrollRadiusSq()) {
            // 现在可以正确调用 getPosTowards，因为它需要 PathfinderMob
            return DefaultRandomPos.getPosTowards(pathfinderMob, 8, 4, Vec3.atCenterOf(home), 1.57D);
        }

        return super.getPosition();
    }
}