package com.dontstravemc.crafting.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CraftRequestPayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<CraftRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dontstravemc", "craft_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeResourceLocation(payload.recipeId()),
            buf -> new CraftRequestPayload(buf.readResourceLocation())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
