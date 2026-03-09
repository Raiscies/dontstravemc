package com.dontstravemc.render.state;

import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoRenderState;

public class TrapRenderState extends GeoRenderState {
    public boolean isTriggered;
    public ItemStack baitItem = ItemStack.EMPTY;
}
