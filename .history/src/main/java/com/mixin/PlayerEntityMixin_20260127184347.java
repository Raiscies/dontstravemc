package com.mixin;

import com.dontstravemc.networking.SanitySyncDataS2CPacket;
import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueOutput;
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
        Player player = (Player) (Object) this;
        // Create a temporary compound tag to hold our data
        CompoundTag tempTag = new CompoundTag();
        CompoundTag sanityTag = new CompoundTag();
        dontstravemc$sanityManager.writeToNbt(sanityTag);

        // Use reflection or direct method call to write to the ValueOutput
        // Since ValueOutput is essentially accepting key-value pairs
        try {
            java.lang.reflect.Method method = valueOutput.getClass().getMethod("put", String.class,
                    net.minecraft.nbt.Tag.class);
            method.invoke(valueOutput, "DontStarveMC", sanityTag);
        } catch (Exception e) {
            // Silently fail if method doesn't exist
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("DontStarveMC")) {
            CompoundTag sanityTag = nbt.getCompound("DontStarveMC").orElse(new CompoundTag());
            dontstravemc$sanityManager.readFromNbt(sanityTag);
        }
    }
}
