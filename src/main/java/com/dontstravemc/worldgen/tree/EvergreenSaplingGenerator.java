package com.dontstravemc.worldgen.tree;

import com.dontstrave;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

public class EvergreenSaplingGenerator {
    
    public static final TreeGrower EVERGREEN = new TreeGrower(
        "evergreen",
        Optional.empty(),
        Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, 
            ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "evergreen_tree"))),
        Optional.empty()
    );
}
