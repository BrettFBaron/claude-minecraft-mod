package com.brettbaron.claudemod.command;

import com.brettbaron.claudemod.ClaudeMod;
import com.brettbaron.claudemod.config.ClaudeConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ApiKeyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("claude-key")
                .requires(source -> source.hasPermissionLevel(4)) // Requires operator permission
                .then(argument("api_key", StringArgumentType.greedyString())
                    .executes(ApiKeyCommand::executeCommand)
                )
        );
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String apiKey = StringArgumentType.getString(context, "api_key");
        
        // Save the API key to config
        ClaudeConfig.saveApiKey(apiKey);
        
        // Confirm to the player
        source.sendFeedback(() -> Text.literal("Claude API key saved successfully"), true);
        ClaudeMod.LOGGER.info("API key updated by " + source.getName());
        
        return 1;
    }
}