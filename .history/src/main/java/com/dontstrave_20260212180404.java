package com;

import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.block.entity.ModBlockEntities;
import com.dontstravemc.command.ModCommands;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.event.ModSounds;
import com.dontstravemc.item.ModItemGroups;
import com.dontstravemc.item.ModItems;
import com.dontstravemc.item.custom.ModCompostables;
import com.dontstravemc.item.custom.ModFuelItem;
import com.dontstravemc.loot.ModLootTableModifiers;
import com.dontstravemc.crafting.TechRecipeManager;
import com.dontstravemc.crafting.networking.ModNetworking;
import com.dontstravemc.status.sanity.SanityLogicHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class dontstrave implements ModInitializer {
    public static final String MOD_ID = "dontstravemc";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModItemGroups.init();
        ModSounds.registerSounds();
        ModLootTableModifiers.register();
        ModEntities.register();
        ModFuelItem.registerFuels();
        ModCompostables.registerCompostables();
        ModCommands.register();
        SanityLogicHandler.register();
        ModNetworking.registerPackets();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(TechRecipeManager.INSTANCE);

        LOGGER.info("Hello The Constant!");
    }
}
