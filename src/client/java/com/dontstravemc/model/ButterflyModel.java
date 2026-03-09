package com.dontstravemc.model;

import com.dontstravemc.animation.ButterflyAnimation;
import com.dontstravemc.render.state.ButterflyRenderState;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.AnimationState;

public class ButterflyModel extends EntityModel<ButterflyRenderState> {
	private final ModelPart root;
	private final KeyframeAnimation bakedFlyAnimation;
	private final KeyframeAnimation bakedStayAnimation;
	private final KeyframeAnimation bakedDieAnimation;
	private final ModelPart bone;
	private final ModelPart Lwing;
	private final ModelPart Lwing2;

	public ButterflyModel(ModelPart root) {
		super(root);
		this.root = root;
		this.bone = root.getChild("bone");
		this.Lwing = this.bone.getChild("Lwing");
		this.Lwing2 = this.bone.getChild("Lwing2");

		this.bakedFlyAnimation = ButterflyAnimation.butterfly_fly.bake(root);
		this.bakedStayAnimation = ButterflyAnimation.butterfly_stay.bake(root);
		this.bakedDieAnimation = ButterflyAnimation.butterfly_die.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();

		// 根骨骼 bone (更新了身体方块和头部)
		PartDefinition bone = partDefinition.addOrReplaceChild("bone", CubeListBuilder.create()
						.texOffs(0, 38).addBox(-1.0F, 0.0F, -6.0F, 2.0F, 1.0F, 12.0F) // 身体
						.texOffs(40, 38).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 2.0F) // 头部
						.texOffs(28, 38).addBox(-1.0F, -3.0F, -11.0F, 2.0F, 4.0F, 4.0F), // 触角/细节
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Lwing = bone.addOrReplaceChild("Lwing", CubeListBuilder.create(),
				PartPose.offset(1F, 0.0F, 0.0F));

		Lwing.addOrReplaceChild("cube_r1", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-1.0F, -0.5F, -10.0F, 9.0F, 0.0F, 19.0F),
				PartPose.offsetAndRotation(1f, 0.0F, 0.0F, 0.0F, 0.0F, -0.48F));

		PartDefinition Lwing2 = bone.addOrReplaceChild("Lwing2", CubeListBuilder.create(),
				PartPose.offset(-1F, 0.0F, 0.0F));

		Lwing2.addOrReplaceChild("cube_r2", CubeListBuilder.create()
						.texOffs(0, 20).addBox(-8.0F, -0.5F, -10.0F, 9.0F, 0.0F, 19.0F),
				PartPose.offsetAndRotation(-1f, 0.0F, 0.0F, 0.0F, 0.0F, 0.48F));

		return LayerDefinition.create(meshDefinition, 64, 64);
	}

	@Override
	public void setupAnim(ButterflyRenderState state) {
		super.setupAnim(state);
		this.root.getAllParts().forEach(ModelPart::resetPose);

		float age = state.ageInTicks;

		if (state.isDead) {
			applyDieAnimation(state, age);
		} else if (state.isResting) {
			applyStayAnimation(state, age);
		} else {
			applyFlyAnimation(state, age);
		}
	}

	private void applyFlyAnimation(ButterflyRenderState state, float age) {
		float manualTime = age * 6f;
		this.bakedFlyAnimation.apply(state.flyAnimationState, manualTime, 1f);
	}

	private void applyStayAnimation(ButterflyRenderState state, float age) {
		float manualTime = age * 4f;
		this.bakedStayAnimation.apply(state.stayAnimationState, manualTime, 1f);
	}

	private void applyDieAnimation(ButterflyRenderState state, float age) {
		float manualTime = age * 1f;
		this.bakedDieAnimation.apply(state.dieAnimationState, manualTime, 1f);
	}
}