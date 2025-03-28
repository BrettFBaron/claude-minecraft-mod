package com.brettbaron.claudemod.command;

import com.brettbaron.claudemod.ClaudeMod;
import com.brettbaron.claudemod.api.ClaudeAPI;
import com.brettbaron.claudemod.api.BlockPlacement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import com.google.gson.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ClaudeCommand {
    // Maximum number of API calls to prevent infinite loops
    private static final int MAX_API_CALLS = 5;
    private static final Gson gson = new Gson();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("claude")
                .then(argument("prompt", StringArgumentType.greedyString())
                    .executes(ClaudeCommand::executeCommand)
                )
        );
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String prompt = StringArgumentType.getString(context, "prompt");
        
        // Send feedback to player that we're processing their request
        source.sendFeedback(() -> Text.literal("Processing: " + prompt), false);
        
        try {
            // Add more detailed logging
            ClaudeMod.LOGGER.info("======= CLAUDE MOD DEBUG =======");
            ClaudeMod.LOGGER.info("Sending prompt to Claude API: " + prompt);
            
            // Get player position for context
            int playerX = (int) source.getPosition().x;
            int playerY = (int) source.getPosition().y;
            int playerZ = (int) source.getPosition().z;
            
            // Construct a more detailed prompt with context
            String contextualPrompt = String.format(
                "Player is at position (%d, %d, %d) in Minecraft and wants: %s", 
                playerX, playerY, playerZ, prompt
            );
            
            // Call Claude API synchronously for easier debugging
            try {
                source.sendFeedback(() -> Text.literal("Making API call to Claude..."), false);
                
                // Make the API call
                String apiResponse = ClaudeAPI.sendRequest(contextualPrompt);
                
                source.sendFeedback(() -> Text.literal("Got response from Claude, processing..."), false);
                
                // Parse the response and extract continuation messages if any
                int totalBlocksPlaced = processFullBuild(apiResponse, source);
                
                // Send success message
                source.sendFeedback(() -> Text.literal("Successfully processed your request!"), false);
                
                return 1;
            } catch (Exception e) {
                ClaudeMod.LOGGER.error("ERROR IN CLAUDE MOD: " + e.getMessage(), e);
                e.printStackTrace(); // Print stack trace to standard output
                
                // Print detailed error to player
                source.sendFeedback(() -> Text.literal("ERROR: " + e.getClass().getName() + ": " + e.getMessage()), false);
                
                // If there's a cause, print that too
                if (e.getCause() != null) {
                    source.sendFeedback(() -> Text.literal("Caused by: " + e.getCause().getMessage()), false);
                }
                
                return 0;
            }
        } catch (Exception e) {
            ClaudeMod.LOGGER.error("ERROR IN CLAUDE MOD COMMAND: " + e.getMessage(), e);
            e.printStackTrace(); // Print stack trace to standard output
            source.sendError(Text.literal("Command error: " + e.getClass().getName() + ": " + e.getMessage()));
            
            // If there's a cause, print that too
            if (e.getCause() != null) {
                source.sendError(Text.literal("Caused by: " + e.getCause().getMessage()));
            }
            
            return 0;
        }
    }
    
    /**
     * Process the complete building process, handling multiple API calls if needed
     */
    private static int processFullBuild(String apiResponse, ServerCommandSource source) {
        ClaudeMod.log("Processing MCS build response");
        
        try {
            // Process the response to extract and execute MCS commands
            int commandsProcessed = BlockPlacement.processResponse(apiResponse, source);
            
            // Final summary
            final int totalCommands = commandsProcessed;
            source.sendFeedback(() -> 
                Text.literal("Built structure with " + totalCommands + " commands!"), false);
            
            return commandsProcessed;
            
        } catch (Exception e) {
            ClaudeMod.log("Error in processFullBuild: " + e.getMessage());
            e.printStackTrace();
            source.sendFeedback(() -> Text.literal("Error processing full build: " + e.getMessage()), false);
            return 0;
        }
    }
    
    // No longer needed for MCS approach - multi-turn building is now handled client-side
}