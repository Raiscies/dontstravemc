package com.dontstravemc.block.entity;

import com.dontstrave;
import com.dontstravemc.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    
    // 使用 Fabric 的 FabricBlockEntityTypeBuilder
    public static final BlockEntityType<RabbitHoleBlockEntity> RABBIT_HOLE_BLOCK_ENTITY = 
        Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "rabbit_hole"),
            FabricBlockEntityTypeBuilder.create(
                RabbitHoleBlockEntity::new,
                ModBlocks.RABBIT_HOLE
            ).build()
        );

    public static void registerBlockEntities() {
        dontstrave.LOGGER.info("Registering Block Entities for " + dontstrave.MOD_ID);
    }
}
