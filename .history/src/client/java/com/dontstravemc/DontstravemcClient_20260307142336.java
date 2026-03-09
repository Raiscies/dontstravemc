package com.dontstravemc;


import com.dontstravemc.block.ModBlocks;
import com.dontstravemc.entity.ModEntities;
import com.dontstravemc.model.ButterflyModel;
import com.dontstravemc.render.renderer.ButterflyRenderer;
import com.dontstravemc.render.renderer.MonsterSpiderRenderer;
import com.dontstravemc.render.renderer.RabbitRenderer;
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
		EntityRendererRegistry.register(ModEntities.RABBIT, RabbitRenderer::new);
		EntityRendererRegistry.register(ModEntities.RABBIT_HOLE, context -> new net.minecraft.client.renderer.entity.EntityRenderer<com.dontstravemc.entity.animal.RabbitHoleEntity, net.minecraft.client.renderer.entity.state.EntityRenderState>(context) {
			@Override
			public net.minecraft.client.renderer.entity.state.EntityRenderState createRenderState() {
				return new net.minecraft.client.renderer.entity.state.EntityRenderState();
			}
		});
		EntityRendererRegistry.register(ModEntities.TRAP, com.dontstravemc.render.renderer.TrapRenderer::new);

		BlockRenderLayerMap.putBlock(ModBlocks.RADIO, ChunkSectionLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(ModBlocks.STICKY_WEBBING, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(ModBlocks.SAPLING, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.SAPLING_WAXED, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.TWIG, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.GRASS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.GRASS_WAXED, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.EVERGREEN_DOOR, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.EVERGREEN_TRAPDOOR, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.EVERGREEN_LEAVES, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.BERRY_BUSH, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.BERRY_BUSH_2, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.BERRY_BUSH_WAXED, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.BERRY_BUSH_2_WAXED, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModBlocks.RABBIT_HOLE, ChunkSectionLayer.CUTOUT);



        SanityHudOverlay.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SanityVisualHandler.update();
        });


	}

}
