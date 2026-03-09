package com.dontstravemc.block.custom;

import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import com.dontstravemc.entity.monster.SpiderDenEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.List;
import com.dontstravemc.entity.util.WebbingOwnershipManager;

public class StickyWebbingBlock extends MultifaceBlock {
    public static final MapCodec<StickyWebbingBlock> CODEC = simpleCodec(StickyWebbingBlock::new);

    public StickyWebbingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends MultifaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Block was actually removed (not just state change)
            if (!level.isClientSide()) {
                WebbingOwnershipManager.getInstance().clearPosition(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }


    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier insideBlockEffectApplier, boolean bl) {
        // 1. 蜘蛛免疫判断 (两端都要运行)
        if (entity instanceof SpiderDenEntity ||
                entity instanceof MonsterSpiderEntity ||
                entity.getType() == EntityType.SPIDER ||
                entity.getType() == EntityType.CAVE_SPIDER){
            return;
        }

        // 2. 精确碰撞检查逻辑
        // 获取方块在世界坐标中的实际形状
        VoxelShape blockShapeInWorld = state.getShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
        // 获取实体的 AABB (已经是世界坐标)，稍微膨胀一点点提高检测灵敏度
        AABB entityBox = entity.getBoundingBox();

        // 检查实体是否真的碰到了蜘蛛网的薄片
        if (Shapes.joinIsNotEmpty(blockShapeInWorld, Shapes.create(entityBox), BooleanOp.AND)) {

            // 3. 【核心修改】减速效果：必须在客户端和服务器同时运行！
            entity.makeStuckInBlock(state, new Vec3(0.6D, 0.05D, 0.6D));

            // 4. 【核心修改】报警逻辑：仅在服务器运行，且只对 LivingEntity 起效
            if (!level.isClientSide() && entity instanceof LivingEntity livingEntity) {
                // 每 20 tick (1秒) 尝试报警一次
                if (level.getGameTime() % 10 == 0) { // 提高到每 0.25 秒检查一次
                    List<SpiderDenEntity> dens = level.getEntitiesOfClass(
                            SpiderDenEntity.class,
                            new AABB(pos).inflate(16.0D),
                            den -> den.isAlive()
                    );

                    if (!dens.isEmpty()) {
                        // 按距离排序，确保呼叫的是最近的那个
                        dens.sort(Comparator.comparingDouble(d -> d.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())));
                        SpiderDenEntity nearestDen = dens.get(0);
                        nearestDen.onWebTriggered(pos, livingEntity);
                    }
                }
            }
        }
    }
}