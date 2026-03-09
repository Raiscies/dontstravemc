package com.dontstravemc.command;

import com.dontstravemc.command.SanityCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModCommands {

    public static void register() {
        // 使用 Fabric API 提供的统一回调入口
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // 在这里统合所有的命令模块
            // 每一个具体的命令类只需要暴露一个静态方法来接收 dispatcher
            SanityCommand.register(dispatcher);
            TechCommand.register(dispatcher);

            // 未来你可以继续在这里添加：
            // HungerCommand.register(dispatcher);
            // WorldCommand.register(dispatcher);
        });
    }
}