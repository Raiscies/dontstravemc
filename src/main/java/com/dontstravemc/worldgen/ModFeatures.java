package com.dontstravemc.worldgen;

import com.dontstrave;
import com.dontstravemc.worldgen.feature.SpiderDenFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModFeatures {
    
    public static Feature<NoneFeatureConfiguration> SPIDER_DEN_FEATURE;
    
    public static void registerModFeatures() {
        dontstrave.LOGGER.info("Registering Features for " + dontstrave.MOD_ID);
        
        SPIDER_DEN_FEATURE = Registry.register(
            BuiltInRegistries.FEATURE,
            ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "spider_den"),
            new SpiderDenFeature(NoneFeatureConfiguration.CODEC)
        );
    }
}
