package com.dontstravemc.worldgen.feature;

import com.dontstrave;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.entity.monster.SpiderDenEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SpiderDenFeature extends Feature<NoneFeatureConfiguration> {
    
    public SpiderDenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        
        dontstrave.LOGGER.info("SpiderDenFeature: Attempting to place at {}", pos);
        
        // WorldGenLevel 在世界生成时使用，我们需要获取底层的 ServerLevel
        ServerLevel serverLevel = level.getLevel();
        
        // Check if the position is valid (solid ground below, air above)
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);
        BlockState currentState = level.getBlockState(pos);
        
        dontstrave.LOGGER.info("SpiderDenFeature: Ground state: {}, Current state: {}", 
            groundState.getBlock().getName().getString(), 
            currentState.getBlock().getName().getString());
        
        // Must have solid ground below and air at spawn position
        if (!groundState.isSolid() || !currentState.isAir()) {
            dontstrave.LOGGER.info("SpiderDenFeature: Invalid placement conditions - solid ground: {}, air above: {}", 
                groundState.isSolid(), currentState.isAir());
            return false;
        }

        // Check if there's enough space (2 blocks high)
        if (!level.getBlockState(pos.above()).isAir()) {
            dontstrave.LOGGER.info("SpiderDenFeature: Not enough vertical space");
            return false;
        }

        // Spawn the spider den entity
        SpiderDenEntity spiderDen = new SpiderDenEntity(ModEntities.SPIDER_DEN, serverLevel);
        
        // Set position using setPos method
        spiderDen.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        
        // Add entity to world
        boolean success = serverLevel.addFreshEntity(spiderDen);
        
        if (success) {
            dontstrave.LOGGER.info("SpiderDenFeature: Successfully spawned spider den at {}", pos);
        } else {
            dontstrave.LOGGER.error("SpiderDenFeature: Failed to add entity to world at {}", pos);
        }
        
        return success;
    }
}
