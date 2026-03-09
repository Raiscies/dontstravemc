package com.dontstravemc.command;


import com.dontstravemc.status.ModComponents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SanityCommand {

    // 修改为接收 dispatcher
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 200))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    float value = FloatArgumentType.getFloat(context, "value");
                                    ModComponents.SANITY.get(player).setSanity(value);

                                    context.getSource().sendSuccess(() ->
                                            Component.translatable("commands.dontstravemc.sanity.set.success", (int)value), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("query")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            float current = ModComponents.SANITY.get(player).getSanity();
                            context.getSource().sendSuccess(() ->
                                    Component.translatable("commands.dontstravemc.sanity.query.success", (int)current), false);
                            return 1;
                        })
                )
        );
    }
}