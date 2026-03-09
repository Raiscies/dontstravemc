package com.dontstravemc.item.custom;

import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.trap.TrapEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrapItem extends Item {
    
    public TrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            BlockPos placePos = pos.relative(direction);
            
            // Create trap entity
            TrapEntity trap = new TrapEntity(ModEntities.TRAP, level);
            trap.setPos(Vec3.atBottomCenterOf(placePos));
            
            // Set uses left based on item damage
            ItemStack stack = context.getItemInHand();
            int damage = stack.getDamageValue();
            int usesLeft = 5 - damage;
            trap.setUsesLeft(usesLeft);
            
            level.addFreshEntity(trap);
            
            // Consume item
            if (!context.getPlayer().isCreative()) {
                stack.shrink(1);
            }
            
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.CONSUME;
    }
}
