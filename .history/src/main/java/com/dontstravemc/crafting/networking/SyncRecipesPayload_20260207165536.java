package com.dontstravemc.crafting.networking;

import com.dontstravemc.crafting.TechRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SyncRecipesPayload(List<TechRecipe> recipes) implements CustomPacketPayload {
    public static final Type<SyncRecipesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dontstravemc", "sync_recipes"));
    
    // We need a way to encode/decode TechRecipe. For now, we'll keep it simple.
    // In a real mod, we'd implement a proper StreamCodec for TechRecipe.
    // For this demonstration, we'll assume the client can load them from resources too,
    // but a sync packet is better practice.
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecipesPayload> CODEC = CustomPacketPayload.codec(
            SyncRecipesPayload::write, SyncRecipesPayload::new
    );

    private SyncRecipesPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readList(b -> {
            // Simplified decoding - in reality would need full codec
            return null; // TODO: Implement full TechRecipe codec
        }));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(recipes, (b, recipe) -> {
            // Simplified encoding
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
