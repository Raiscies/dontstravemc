package com.dontstravemc.model;

import com.dontstravemc.entity.monster.MonsterSpiderEntity;
import com.dontstravemc.render.state.MonsterSpiderRenderState;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class MonsterSpiderModel extends GeoModel<MonsterSpiderEntity> {
	private ResourceLocation spiderTex(String name) {
		return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/spider/spider_" + name + ".png");
	}

	@Override
	public ResourceLocation getModelResource(GeoRenderState renderState) {
		return ResourceLocation.fromNamespaceAndPath("dontstravemc", "entity/monster_spider.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GeoRenderState renderState) {
		if (renderState instanceof MonsterSpiderRenderState s) {
			if (s.isDead) return spiderTex("dead");
			if (s.isHurt) return spiderTex("hurt");
			if (s.isAggressive) return spiderTex("aggressive");
			if (s.isAsleep) return spiderTex("sleeping");
			if (s.isEating) return spiderTex("eating");
		}

		// 默认贴图
		return ResourceLocation.fromNamespaceAndPath("dontstravemc", "textures/entity/spider/spider.png");
	}

	@Override
	public ResourceLocation getAnimationResource(MonsterSpiderEntity animatable) {
		// 注意：只有动画资源依然接收实体实例 (T animatable)
		return ResourceLocation.fromNamespaceAndPath("dontstravemc", "entity/monster_spider");
	}
}