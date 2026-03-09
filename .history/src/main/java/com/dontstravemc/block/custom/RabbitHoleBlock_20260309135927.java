package com.dontstravemc.block.custom;

import com.dontstravemc.block.entity.ModBlockEntities;
import com.dontstravemc.block.entity.RabbitHoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 兔子洞方块 - 参考原版蜂巢设计
 * 使用 BlockEntity 系统管理兔子的存储和生成
 */
public class RabbitHoleBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 4.8, 12.0);
    
    public RabbitHoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockPos below = pos.below();
        BlockState belowState = context.getLevel().getBlockState(below);
        
        // 只能在完整方块上放置
        if (!belowState.isFaceSturdy(context.getLevel(), below, Direction.UP)) {
            return null;
        }
        
        return super.getStateForPlacement(context);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // 当玩家破坏方块时，释放所有兔子
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RabbitHoleBlockEntity blockEntity) {
            blockEntity.releaseAllRabbits((ServerLevel) level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ========== BlockEntity 相关方法 ==========

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RabbitHoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 只在服务端运行ticker
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.RABBIT_HOLE_BLOCK_ENTITY, RabbitHoleBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 使用普通模型渲染（不是不可见的）
        return RenderShape.MODEL;
    }
}
