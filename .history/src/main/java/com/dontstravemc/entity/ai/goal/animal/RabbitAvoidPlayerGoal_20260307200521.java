package com.dontstravemc.entity.ai.goal.animal;

import com.dontstravemc.entity.animal.RabbitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RabbitAvoidPlayerGoal extends Goal {
    private final RabbitEntity rabbit;
    private final double fleeSpeed;
    private Player targetPlayer;
    private boolean hasScreamed = false;

    public RabbitAvoidPlayerGoal(RabbitEntity rabbit, double fleeSpeed) {
        this.rabbit = rabbit;
        this.fleeSpeed = fleeSpeed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.rabbit.isSleeping() && this.rabbit.getLastHurtByMob() == null) {
            return false;
        }

        if (this.rabbit.shouldReturnHome() && this.rabbit.hasHome()) {
            return false;
        }
        
        this.targetPlayer = this.rabbit.level().getNearestPlayer(this.rabbit, 10.0D);
        if (this.targetPlayer == null) {
            this.rabbit.setAlert(false);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null) return false;
        double dist = this.rabbit.distanceTo(this.targetPlayer);
        // 继续逃跑直到距离达到12格以上
        return dist < 12.0D;
    }

    @Override
    public void start() {
        this.hasScreamed = false;
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;
        
        double dist = this.rabbit.distanceTo(this.targetPlayer);

        // 8格内触发惊吓和奔跑
        if (dist < 8.0D) {
            // 只在首次进入8格内且还没尖叫过时触发音效
            if (!this.hasScreamed) {
                this.rabbit.triggerScream();
                this.hasScreamed = true;
            }
            
            // 进入奔跑模式，一直跑到12格外
            this.rabbit.setAlert(false);
            if (this.rabbit.getNavigation().isDone() || this.rabbit.tickCount % 5 == 0) {
                Vec3 fleeVec = this.calculateFleeVector();
                if (fleeVec != null) {
                    this.rabbit.getNavigation().moveTo(fleeVec.x, fleeVec.y, fleeVec.z, this.fleeSpeed);
                }
            }
        } else if (dist < 10.0D) {
            // 8-10格之间：继续奔跑但不触发音效
            this.rabbit.setAlert(false);
            if (this.rabbit.getNavigation().isDone() || this.rabbit.tickCount % 5 == 0) {
                Vec3 fleeVec = this.calculateFleeVector();
                if (fleeVec != null) {
                    this.rabbit.getNavigation().moveTo(fleeVec.x, fleeVec.y, fleeVec.z, this.fleeSpeed);
                }
            }
        } else {
            // 10-12格之间：警戒观察
            this.rabbit.setAlert(true);
            this.rabbit.getNavigation().stop();
            this.rabbit.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
        }
    }

    private Vec3 calculateFleeVector() {
        Vec3 rabbitPos = this.rabbit.position();
        Vec3 playerPos = this.targetPlayer.position();
        
        // Direction away from player
        Vec3 awayFromPlayer = rabbitPos.subtract(playerPos).normalize();
        
        if (this.rabbit.hasHome()) {
            BlockPos homePos = this.rabbit.getTether().getHomePos();
            Vec3 homeVec = new Vec3(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5);
            Vec3 toHome = homeVec.subtract(rabbitPos).normalize();
            
            // If home is in sight and somewhat in the same direction, go straight to home
            // Otherwise, combine vectors to "arc" around player
            // Elliptical tendency: prioritize home, but nudge 'away' from player
            Vec3 combined = toHome.scale(2.0).add(awayFromPlayer).normalize();
            
            // Check if combined direction is too close to player
            double dot = combined.dot(playerPos.subtract(rabbitPos).normalize());
            if (dot > -0.2) {
                // We are trying to run THROUGH the player. Nudge sideways.
                Vec3 nudge = new Vec3(-combined.z, combined.y, combined.x); // Perpendicular
                combined = combined.add(nudge).normalize();
            }
            
            return rabbitPos.add(combined.scale(4.0));
        }

        // Just run away if no home
        return rabbitPos.add(awayFromPlayer.scale(4.0));
    }

    @Override
    public void stop() {
        this.rabbit.setAlert(false);
        this.targetPlayer = null;
    }
}
