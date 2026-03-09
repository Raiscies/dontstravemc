package com.mixin;

import com.dontstravemc.networking.SanitySyncDataS2CPacket;
import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements SanityAccess {

    @Unique
    private final SanityManager dontstravemc$sanityManager = new SanityManager();

    @Override
    public SanityManager getSanityManager() {
        return dontstravemc$sanityManager;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Only run on server side
        if (!player.level().isClientSide()) {
            dontstravemc$sanityManager.tick();

            // Check if we should sync to client
            if (dontstravemc$sanityManager.shouldSync() && player instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(
                        serverPlayer,
                        new SanitySyncDataS2CPacket(dontstravemc$sanityManager.getSanity()));
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onWriteCustomDataToNbt(ValueOutput valueOutput, CallbackInfo ci) {
        CompoundTag sanityTag = new CompoundTag();
        dontstravemc$sanityManager.writeToNbt(sanityTag);

        // Use reflection to write to the ValueOutput
        try {
            java.lang.reflect.Method method = valueOutput.getClass().getMethod("put", String.class,
                    net.minecraft.nbt.Tag.class);
            method.invoke(valueOutput, "DontStarveMC", sanityTag);
        } catch (Exception e) {
            // Silently fail if method doesn't exist
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadCustomDataFromNbt(ValueInput valueInput, CallbackInfo ci) {
        // Cast to Object first to bypass strict type check if ValueInput and
        // CompoundTag are not directly related in hierarchy
        if ((Object) valueInput instanceof CompoundTag) {
            CompoundTag nbt = (CompoundTag) (Object) valueInput;
            if (nbt.contains("DontStarveMC")) {
                // Assuming getCompound returns Optional based on previous context
                // But we need to handle if it returns CompoundTag directly to be safe
                try {
                    Object tagObj = nbt.getCompound("DontStarveMC");
                    CompoundTag sanityTag;
                    if (tagObj instanceof java.util.Optional) {
                        sanityTag = ((java.util.Optional<CompoundTag>) tagObj).orElse(new CompoundTag());
                    } else {
                        sanityTag = (CompoundTag) tagObj;
                    }
                    dontstravemc$sanityManager.readFromNbt(sanityTag);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}
