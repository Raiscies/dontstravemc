package com.dontstravemc.block.custom;

import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.animal.RabbitHoleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RabbitHoleBlock extends Block {
    public RabbitHoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, level, pos, oldState, notify);
        if (!level.isClientSide()) {
            ensureEntityExists((ServerLevel) level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            ensureEntityExists((ServerLevel) level, pos);
        }
    }

    /**
     * Ensures that a RabbitHoleEntity exists at this position.
     * This is a simple logical anchor for the ReturnToDenGoal.
     */
    private void ensureEntityExists(ServerLevel level, BlockPos pos) {
        List<RabbitHoleEntity> entities = level.getEntitiesOfClass(RabbitHoleEntity.class, new AABB(pos).inflate(0.5));
        if (entities.isEmpty()) {
            RabbitHoleEntity entity = ModEntities.RABBIT_HOLE.create(level, EntitySpawnReason.TRIGGERED);
            if (entity != null) {
                entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                entity.setPersistenceRequired();
                level.addFreshEntity(entity);
            }
        }
    }
}
