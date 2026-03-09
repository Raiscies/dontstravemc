package com;

import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.command.ModCommands;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.event.ModSounds;
import com.dontstravemc.item.ModItemGroups;
import com.dontstravemc.item.ModItems;
import com.dontstravemc.item.custom.ModCompostables;
import com.dontstravemc.item.custom.ModFuelItem;
import com.dontstravemc.loot.ModLootTableModifiers;
import com.dontstravemc.item.FertilizerHelper;
import com.dontstravemc.crafting.TechRecipeManager;
import com.dontstravemc.crafting.networking.ModNetworking;
import com.dontstravemc.status.sanity.SanityLogicHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class dontstrave implements ModInitializer {
    public static final String MOD_ID = "dontstravemc";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        com.dontstravemc.block.entity.ModBlockEntities.registerBlockEntities();
        ModItemGroups.init();
        ModSounds.registerSounds();
        ModLootTableModifiers.register();
        ModEntities.register();
        com.dontstravemc.worldgen.ModFeatures.registerModFeatures();
        ModFuelItem.registerFuels();
        ModCompostables.registerCompostables();
        ModCommands.register();
        SanityLogicHandler.register();
        ModNetworking.registerPackets();
        FertilizerHelper.registerFertilizers();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TechRecipeManager.INSTANCE);

        LOGGER.info("Hello The Constant!");
    }
}
