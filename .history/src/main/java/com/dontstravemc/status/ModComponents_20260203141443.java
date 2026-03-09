package com.dontstravemc.status;

import com.dontstravemc.status.sanity.PlayerSanityComponent;
import com.dontstravemc.status.sanity.SanityComponents;
import net.minecraft.resources.ResourceLocation;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class ModComponents implements EntityComponentInitializer {
    // 静态检索键实例
    public static final ComponentKey<SanityComponents> SANITY =
            ComponentRegistry.getOrCreate(
                    ResourceLocation.fromNamespaceAndPath("dontstravemc", "sanity"),
                    SanityComponents.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(SANITY, PlayerSanityComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}