package com.dontstravemc;


import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.model.ButterflyModel;
import com.dontstravemc.render.renderer.ButterflyRenderer;
import com.dontstravemc.render.renderer.MonsterSpiderRenderer;

import com.dontstravemc.render.renderer.SpiderDenRenderer;
import com.dontstravemc.status.SanityHudOverlay;
import com.dontstravemc.status.SanityVisualHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import com.dontstravemc.block.entity.ModBlockEntities;
import com.dontstravemc.render.renderer.TwigBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;

public class DontstravemcClient implements ClientModInitializer {

	public static final ModelLayerLocation MONSTER_SPIDER_LAYER = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("dontstravemc", "monster_spider"), "main");
	public static final ModelLayerLocation BUTTERFLY_LAYER = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("dontstravemc", "butterfly"), "main");

	@Override
	public void onInitializeClient() {

		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
			if (id != null && id.getNamespace().equals("dontstravemc")) {
				lines.add(Component.translatable(
						"tooltip." + id.getNamespace() + "." + id.getPath()));
			}
		});

		EntityRendererRegistry.register(ModEntities.MONSTER_SPIDER, MonsterSpiderRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPIDER_DEN, SpiderDenRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(BUTTERFLY_LAYER, ButterflyModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BUTTERFLY, ButterflyRenderer::new);

		BlockRenderLayerMap.putBlock(ModBlocks.RADIO, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.STICKY_WEBBING, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(ModBlocks.SAPLING, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TWIG, ChunkSectionLayer.CUTOUT);



        SanityHudOverlay.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SanityVisualHandler.update();
        });


	}

}
