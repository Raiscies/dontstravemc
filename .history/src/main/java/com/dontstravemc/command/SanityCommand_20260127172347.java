package com.dontstravemc.command;

import com.dontstravemc.networking.SanitySyncDataS2CPacket;
import com.dontstravemc.sanity.SanityAccess;
import com.dontstravemc.sanity.SanityManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SanityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanity")
                .requires(source -> source.hasPermission(2)) // Requires cheats/op
                .then(Commands.literal("add")
                        .then(Commands.argument("delta", FloatArgumentType.floatArg())
                                .executes(SanityCommand::addSanity)))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                .executes(SanityCommand::setSanity))));
    }

    private static int addSanity(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        float delta = FloatArgumentType.getFloat(context, "delta");
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            manager.addSanity(delta);
            manager.markSynced();

            // Sync to client immediately
            ServerPlayNetworking.send(player, new SanitySyncDataS2CPacket(manager.getSanity()));

            context.getSource().sendSuccess(
                    () -> Component.literal("Sanity changed by " + delta + ". Current: " + manager.getSanity()),
                    false);
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFailure(Component.literal("Failed to modify sanity"));
        return 0;
    }

    private static int setSanity(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        float value = FloatArgumentType.getFloat(context, "value");
        ServerPlayer player = context.getSource().getPlayerOrException();

        if (player instanceof SanityAccess sanityAccess) {
            SanityManager manager = sanityAccess.getSanityManager();
            manager.setSanity(value);
            manager.markSynced();

            // Sync to client immediately
            ServerPlayNetworking.send(player, new SanitySyncDataS2CPacket(manager.getSanity()));

            context.getSource().sendSuccess(
                    () -> Component.literal("Sanity set to " + value),
                    false);
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFailure(Component.literal("Failed to set sanity"));
        return 0;
    }
}
