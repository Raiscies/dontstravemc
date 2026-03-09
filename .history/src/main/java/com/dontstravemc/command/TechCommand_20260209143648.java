package com.dontstravemc.command;

import com.dontstravemc.status.ModComponents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TechCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tech")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("level")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 10))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            ModComponents.KNOWLEDGE.get(player).setTechLevel(value);

                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("commands.dontstravemc.tech.level.set.success", value), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-10, 10))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            int current = ModComponents.KNOWLEDGE.get(player).getTechLevel();
                                            ModComponents.KNOWLEDGE.get(player).setTechLevel(current + value);

                                            context.getSource().sendSuccess(() ->
                                                    Component.translatable("commands.dontstravemc.tech.level.add.success", value), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("query")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int current = ModComponents.KNOWLEDGE.get(player).getTechLevel();
                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("commands.dontstravemc.tech.level.query.success", current), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}
