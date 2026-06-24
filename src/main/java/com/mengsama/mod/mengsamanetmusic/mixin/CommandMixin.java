package com.mengsama.mod.mengsamanetmusic.mixin;

import com.mengsama.mod.mengsamanetmusic.config.ConfigManager;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.RegisterCommandsEvent;
import net.fabricmc.fabric.api.SubscribeEvent;
import net.fabricmc.fabric.common.Mod;

@Mod.EventBusSubscriber
public class CommandMixin {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("mengsamanetmusic")
                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(CommandMixin::mengsamanetmusic$reload))
                        .then(Commands.literal("cache")
                                .then(Commands.argument("id", LongArgumentType.longArg())
                                        .executes(CommandMixin::mengsamanetmusic$cache)))
        );
    }

    private static int mengsamanetmusic$reload(CommandContext<CommandSourceStack> context) {
        ConfigManager.reload();
        context.getSource().sendSuccess(() -> Component.literal("MengSamaNetMusic config reloaded."), false);
        return 1;
    }

    private static int mengsamanetmusic$cache(CommandContext<CommandSourceStack> context) {
        long id = LongArgumentType.getLong(context, "id");
        context.getSource().sendSuccess(() -> Component.literal("Caching music ID: " + id), false);
        return 1;
    }
}
