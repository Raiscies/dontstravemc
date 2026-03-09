package com.dontstravemc.animation;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.Keyframe;
import org.joml.Vector3f;

public class ButterflyAnimation {
	private static Vector3f rot(float x, float y, float z) {
		float f = (float)Math.PI / 180F;
		return new Vector3f(x * f, y * f, z * f);
	}

	public static final AnimationDefinition butterfly_fly = AnimationDefinition.Builder.withLength(1.5F).looping()
			.addAnimation("Lwing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.25F, rot(0.0F, 0.0F, -40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.75F, rot(0.0F, 0.0F, 40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, rot(0.0F, 0.0F, 80F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.25F, rot(0.0F, 0.0F, 40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.5F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("Lwing2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.25F, rot(0.0F, 0.0F, 40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.75F, rot(0.0F, 0.0F, -40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, rot(0.0F, 0.0F, -80F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.25F, rot(0.0F, 0.0F, -40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.5F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();


	public static final AnimationDefinition butterfly_stay = AnimationDefinition.Builder.withLength(1F).looping()
			.addAnimation("Lwing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, 40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("Lwing2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, -40F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();


	public static final AnimationDefinition butterfly_die = AnimationDefinition.Builder.withLength(0.5F)
			.addAnimation("Lwing", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, -70.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("Lwing2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.0F, rot(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, rot(0.0F, 0.0F, 70.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();
}