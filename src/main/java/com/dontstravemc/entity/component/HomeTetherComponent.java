package com.dontstravemc.entity.component;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class HomeTetherComponent {
    private BlockPos homePos;
    private double strollRadius;
    private double chaseRadius;
    private double maxTetherDist;

    public HomeTetherComponent(double stroll, double chase, double max) {
        this.strollRadius = stroll;
        this.chaseRadius = chase;
        this.maxTetherDist = max;
    }

    public void setHomePos(BlockPos pos) { this.homePos = pos; }
    public BlockPos getHomePos() { return homePos; }

    public double getStrollRadiusSq() { return strollRadius * strollRadius; }
    public double getChaseRadiusSq() { return chaseRadius * chaseRadius; }
    public double getMaxTetherDistSq() { return maxTetherDist * maxTetherDist; }

    public boolean isTooFar(Mob mob) {
        if (homePos == null) return false;
        return mob.distanceToSqr(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5) > getMaxTetherDistSq();
    }

    // 适配 1.21 ValueOutput
    public void toNBT(ValueOutput out) {
        if (homePos != null) out.store("HomePos", BlockPos.CODEC, homePos);
    }

    // 适配 1.21 ValueInput
    public void fromNBT(ValueInput in) {
        in.read("HomePos", BlockPos.CODEC).ifPresent(pos -> this.homePos = pos);
    }
}
