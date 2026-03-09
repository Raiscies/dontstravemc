package com.dontstravemc.entity.util.stateutil;

import com.dontstravemc.entity.component.HomeTetherComponent;
import net.minecraft.core.BlockPos;

public interface Tetherable {
    HomeTetherComponent getTether();
    void setHomePos(BlockPos pos);
    boolean shouldReturnHome(); // 返回是否是该回家的时间（白天）
    boolean hasHome(); // 是否有关联的巢穴
}
