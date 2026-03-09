package com.dontstravemc.block.entity;

import com.dontstrave;
import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.block.entity.custom.TwigBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<TwigBlockEntity> TWIG_BLOCK_ENTITY =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "twig_block_entity"),
                    FabricBlockEntityTypeBuilder.create(TwigBlockEntity::new, ModBlocks.TWIG).build());

    public static void registerBlockEntities() {
        dontstrave.LOGGER.info("Registering Block Entities for " + dontstrave.MOD_ID);
    }
}
