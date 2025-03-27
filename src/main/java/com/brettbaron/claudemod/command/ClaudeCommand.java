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
        ClaudeMod.log("Processing full build response");
        int totalBlocksPlaced = 0;
        int apiCallsMade = 1;  // We've already made one call at this point
        
        try {
            // Process initial response
            int blocksPlaced = BlockPlacement.processResponse(apiResponse, source);
            totalBlocksPlaced += blocksPlaced;
            
            // Let the player know progress
            if (blocksPlaced > 0) {
                final int initialBlocks = blocksPlaced;
                final int initialPhase = apiCallsMade;
                source.sendFeedback(() -> 
                    Text.literal("Progress: Placed " + initialBlocks + " blocks in phase " + initialPhase), false);
            }
            
            // Check if there are more parts to build in the response
            JsonObject responseJson = gson.fromJson(apiResponse, JsonObject.class);
            boolean hasMoreContent = hasMoreBuildingContent(responseJson);
            
            // Continue processing more API calls if needed
            while (hasMoreContent && apiCallsMade < MAX_API_CALLS) {
                ClaudeMod.log("Detected more building content, making additional API call");
                final int nextPhase = apiCallsMade + 1;
                source.sendFeedback(() -> Text.literal("Building continues... (Phase " + nextPhase + ")"), false);
                
                // Extract continuation message or create one
                String continuationPrompt = getContinuationPrompt(responseJson);
                ClaudeMod.log("Using continuation prompt: " + continuationPrompt);
                
                // Make another API call
                apiCallsMade++;
                String nextResponse = ClaudeAPI.sendRequest(continuationPrompt);
                
                // Process this response
                blocksPlaced = BlockPlacement.processResponse(nextResponse, source);
                totalBlocksPlaced += blocksPlaced;
                
                // Let the player know progress
                if (blocksPlaced > 0) {
                    final int phaseBlocks = blocksPlaced;
                    final int phase = apiCallsMade;
                    source.sendFeedback(() -> 
                        Text.literal("Progress: Placed " + phaseBlocks + " blocks in phase " + phase), false);
                }
                
                // Check if there are still more parts
                responseJson = gson.fromJson(nextResponse, JsonObject.class);
                hasMoreContent = hasMoreBuildingContent(responseJson);
            }
            
            // If we reached the maximum number of API calls, let the user know
            if (apiCallsMade >= MAX_API_CALLS && hasMoreBuildingContent(responseJson)) {
                source.sendFeedback(() -> 
                    Text.literal("Warning: Building reached maximum number of phases (" + MAX_API_CALLS + ")"), false);
            }
            
            // Final summary
            final int finalTotal = totalBlocksPlaced;
            final int finalPhases = apiCallsMade;
            source.sendFeedback(() -> 
                Text.literal("Built structure with " + finalTotal + " total blocks across " + 
                             finalPhases + " building phases!"), false);
            
            return totalBlocksPlaced;
            
        } catch (Exception e) {
            ClaudeMod.log("Error in processFullBuild: " + e.getMessage());
            e.printStackTrace();
            source.sendFeedback(() -> Text.literal("Error processing full build: " + e.getMessage()), false);
            return totalBlocksPlaced;
        }
    }
    
    /**
     * Check if the response indicates there are more blocks to place
     */
    private static boolean hasMoreBuildingContent(JsonObject responseJson) {
        try {
            // If the response has a text content with certain keywords indicating more to build
            if (responseJson.has("content") && responseJson.get("content").isJsonArray()) {
                JsonArray contentArray = responseJson.getAsJsonArray("content");
                
                for (JsonElement element : contentArray) {
                    if (element.isJsonObject()) {
                        JsonObject contentObj = element.getAsJsonObject();
                        
                        if (contentObj.has("type") && contentObj.get("type").getAsString().equals("text")) {
                            if (contentObj.has("text")) {
                                String text = contentObj.get("text").getAsString().toLowerCase();
                                
                                // Look for phrases indicating more building to come
                                if (text.contains("next section") || 
                                    text.contains("continue building") || 
                                    text.contains("next part") ||
                                    text.contains("moving on to") ||
                                    text.contains("now let's build") ||
                                    text.contains("let's add") ||
                                    text.contains("next, we'll")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            ClaudeMod.log("Error checking for more building content: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a prompt for continuing the building process
     */
    private static String getContinuationPrompt(JsonObject responseJson) {
        try {
            StringBuilder prompt = new StringBuilder("Please continue building the structure. ");
            prompt.append("Send the next set of blocks to place using the place_blocks tool. ");
            
            // Extract the last message to provide context
            if (responseJson.has("content") && responseJson.get("content").isJsonArray()) {
                JsonArray contentArray = responseJson.getAsJsonArray("content");
                
                for (int i = contentArray.size() - 1; i >= 0; i--) {
                    JsonElement element = contentArray.get(i);
                    if (element.isJsonObject()) {
                        JsonObject contentObj = element.getAsJsonObject();
                        
                        if (contentObj.has("type") && contentObj.get("type").getAsString().equals("text")) {
                            if (contentObj.has("text")) {
                                String text = contentObj.get("text").getAsString();
                                // Extract last 200 characters for context
                                if (text.length() > 200) {
                                    prompt.append("Continuing from: \"...");
                                    prompt.append(text.substring(text.length() - 200));
                                    prompt.append("\"");
                                } else {
                                    prompt.append("Continuing from: \"");
                                    prompt.append(text);
                                    prompt.append("\"");
                                }
                                break;
                            }
                        }
                    }
                }
            }
            
            return prompt.toString();
        } catch (Exception e) {
            ClaudeMod.log("Error creating continuation prompt: " + e.getMessage());
            return "Please continue building the structure. Send the next set of blocks to place.";
        }
    }
}