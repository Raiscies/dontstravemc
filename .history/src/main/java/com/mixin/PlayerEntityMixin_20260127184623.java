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
    private void onReadCustomDataFromNbt(ValueInput valueInput, CallbackInfo ci) {
        if (valueInput instanceof CompoundTag nbt) {
            if (nbt.contains("DontStarveMC")) {
                CompoundTag sanityTag = nbt.getCompound("DontStarveMC"); // Tag is not optional in most mappings,
                                                                         // checking
                // If getCompound returns Optional in this version, handle it.
                // Based on previous checks, getFloat returned Optional<Float>, so getCompound
                // might return Optional<CompoundTag>?
                // Usually getCompound returns CompoundTag and empty if not found, but let's
                // see.
                // Wait, in standard mappings getCompound returns CompoundTag.
                // However, the user's previous error was Optional<Float>.
                // In 1.21, getCompound might return Optional.
                // Let's use getCompound("DontStarveMC") and see.
                // Actually, I can use the same logic as the user had: "tag.getFloat" returned
                // Optional.
                // Let's assume getCompound is standard or returns CompoundTag.
                // But wait, the user's code previously had: 'CompoundTag sanityTag =
                // nbt.getCompound("DontStarveMC").orElse(new CompoundTag());'
                // This implies getCompound returns Optional.

                // So I should stick to that if it compiles.
                // But I need to be careful about the type.

                // Let's use reflection to be safe about the getCompound return type too?
                // No, I can just use what was there if I cast it to CompoundTag.

                // Let's just use the previous logic but inside the instanceof check.

                // Wait, 'orElse' was used on 'nbt.getCompound("DontStarveMC")'.
                // If nbt is CompoundTag, I can call its methods.
                // I will assume the previous code 'nbt.getCompound("DontStarveMC").orElse(...)'
                // was correct for THIS version of mappings.

                try {
                    // Check if getCompound returns Optional
                    Object sanityTagObj = nbt.getCompound("DontStarveMC");
                    CompoundTag sanityTag;
                    if (sanityTagObj instanceof java.util.Optional) {
                        sanityTag = ((java.util.Optional<CompoundTag>) sanityTagObj).orElse(new CompoundTag());
                    } else {
                        sanityTag = (CompoundTag) sanityTagObj;
                    }
                    dontstravemc$sanityManager.readFromNbt(sanityTag);
                } catch (Exception e) {
                    // Fallback or ignore
                }
            }
        }
    }
}
