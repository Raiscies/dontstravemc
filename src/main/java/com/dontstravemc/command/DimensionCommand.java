package com.dontstravemc.command;

import com.dontstrave;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class DimensionCommand {
    
    public static final ResourceKey<Level> THE_CONSTANT = ResourceKey.create(
        net.minecraft.core.registries.Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(dontstrave.MOD_ID, "the_constant")
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("theconstant")
            .requires(source -> source.hasPermission(2))
            .executes(DimensionCommand::teleportToConstant));
    }

    private static int teleportToConstant(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.dontstravemc.dimension.player_only"));
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(THE_CONSTANT);
        
        if (targetLevel == null) {
            source.sendFailure(Component.translatable("commands.dontstravemc.dimension.not_found"));
            return 0;
        }

        // Find a safe spawn position at y=4 (above the surface layers)
        BlockPos spawnPos = new BlockPos(0, 4, 0);
        Vec3 targetPos = spawnPos.getCenter();
        
        // Create teleport transition
        TeleportTransition transition = new TeleportTransition(
            targetLevel,
            targetPos,
            Vec3.ZERO,
            player.getYRot(),
            player.getXRot(),
            Set.of(),
            TeleportTransition.DO_NOTHING
        );
        
        // Teleport player
        ServerPlayer result = player.teleport(transition);
        
        if (result != null) {
            source.sendSuccess(() -> Component.translatable("commands.dontstravemc.dimension.success"), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("commands.dontstravemc.dimension.not_found"));
            return 0;
        }
    }
}
