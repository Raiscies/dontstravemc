package com.dontstravemc.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SanitySyncDataS2CPacket(float sanity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SanitySyncDataS2CPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("dontstravemc", "sanity_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SanitySyncDataS2CPacket> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.FLOAT,
            SanitySyncDataS2CPacket::sanity,
            SanitySyncDataS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }
}
