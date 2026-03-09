package com.dontstravemc.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.item.Items.registerBlock;

public class RadioBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RadioBlock> CODEC = simpleCodec(RadioBlock::new);

    // 计算逻辑：
    // 长宽14：(16 - 14) / 2 = 1.0，所以范围是 1.0 到 15.0
    // 高度13：范围是 0.0 到 13.0
    // 因为长宽相等(14x14)，其实四个方向的形状是一样的。如果后续模型改为非对称，再调整数值。
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

    public RadioBlock(BlockBehaviour.Properties properties) {
        // 建议在外部注册时调用 .noOcclusion()，或者确保 properties 已经设置了非全方位不透明
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }


    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    // 重写此方法来应用碰撞箱/选框
    // 在 Mojmap 中，该方法名为 getShape
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, net.minecraft.core.BlockPos pos, CollisionContext context) {
        // 如果你的模型是正方形底座，直接返回 SHAPE 即可
        // 如果模型将来改为长方形（如 14x10），则需要根据 state.getValue(FACING) 切换不同的 SHAPE 变量
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}