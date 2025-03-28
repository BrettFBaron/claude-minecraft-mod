package com.brettbaron.claudemod.api;

import com.brettbaron.claudemod.ClaudeMod;
import com.brettbaron.claudemod.mcs.McsProcessor;
import com.google.gson.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles the processing of Claude's MCS (Minecraft Command Syntax) responses
 */
public class BlockPlacement {
    private static final Gson gson = new Gson();
    
    /**
     * Process a response from Claude API and extract MCS commands
     * 
     * @param apiResponse Raw JSON response from Claude API
     * @param source The command source for getting the world context
     * @return Number of commands prepared for execution
     */
    public static int processResponse(String apiResponse, ServerCommandSource source) {
        try {
            ClaudeMod.log("Processing Claude API response for MCS commands");
            JsonObject responseJson = gson.fromJson(apiResponse, JsonObject.class);
            
            // Check if there was an error in the response
            if (responseJson.has("error")) {
                String errorMessage = responseJson.getAsJsonObject("error").get("message").getAsString();
                ClaudeMod.log("API error: " + errorMessage);
                source.sendFeedback(() -> Text.literal("Error from Claude API: " + errorMessage), false);
                return 0;
            }
            
            // First, check for tool usage that might contain MCS commands
            String mcsCommands = null;
            
            // Look for content array
            if (!responseJson.has("content")) {
                ClaudeMod.LOGGER.error("Invalid API response format: missing content");
                throw new JsonParseException("Invalid API response format: missing content");
            }
            
            // Check if content is an array
            JsonElement contentElement = responseJson.get("content");
            if (!contentElement.isJsonArray()) {
                ClaudeMod.log("Invalid API response format: content is not an array");
                throw new JsonParseException("Invalid API response format: content is not an array");
            }
            
            JsonArray contentArray = contentElement.getAsJsonArray();
            
            // Look for generate_mcs tool calls
            for (JsonElement contentEl : contentArray) {
                if (!contentEl.isJsonObject()) continue;
                
                JsonObject contentObj = contentEl.getAsJsonObject();
                
                if (contentObj.has("type") && contentObj.get("type").getAsString().equals("tool_use")) {
                    if (contentObj.has("name") && contentObj.get("name").getAsString().equals("generate_mcs")) {
                        if (contentObj.has("input") && contentObj.get("input").isJsonObject()) {
                            JsonObject input = contentObj.get("input").getAsJsonObject();
                            if (input.has("commands")) {
                                mcsCommands = input.get("commands").getAsString();
                                break;
                            }
                        }
                    }
                }
            }
            
            // If we didn't find MCS commands via tool usage, extract from the text content
            if (mcsCommands == null || mcsCommands.isEmpty()) {
                // Extract message content as a raw string
                String rawContent = extractTextContent(responseJson);
                
                // Create MCS file from raw content
                String buildName = "build_" + UUID.randomUUID().toString().substring(0, 8);
                String mcsFilePath = McsProcessor.createMcsFromResponse(rawContent, buildName);
                
                // Execute the MCS file
                source.sendFeedback(() -> Text.literal("Created MCS file: " + mcsFilePath), false);
                source.sendFeedback(() -> Text.literal("Executing MCS commands..."), false);
                
                // Start execution and count commands
                int commandCount = countCommands(mcsFilePath);
                McsProcessor.executeMcsFile(mcsFilePath, source);
                
                return commandCount;
            } else {
                // We have MCS commands from tool usage, save and execute them
                String buildName = "build_" + UUID.randomUUID().toString().substring(0, 8);
                String mcsFilePath = McsProcessor.saveMcsFile(buildName, mcsCommands);
                
                // Execute the MCS file
                source.sendFeedback(() -> Text.literal("Created MCS file: " + mcsFilePath), false);
                source.sendFeedback(() -> Text.literal("Executing MCS commands..."), false);
                
                // Start execution and count commands
                int commandCount = countCommands(mcsFilePath);
                McsProcessor.executeMcsFile(mcsFilePath, source);
                
                return commandCount;
            }
            
        } catch (JsonParseException e) {
            ClaudeMod.log("Error parsing Claude API response: " + e.getMessage());
            source.sendFeedback(() -> Text.literal("Error: Could not parse Claude's response - " + e.getMessage()), false);
            return 0;
        } catch (Exception e) {
            ClaudeMod.log("Error processing MCS commands: " + e.getMessage());
            e.printStackTrace();
            source.sendFeedback(() -> Text.literal("Error processing MCS commands: " + e.getMessage()), false);
            return 0;
        }
    }
    
    /**
     * Extract text content from Claude's response
     * 
     * @param jsonResponse The JSON response object
     * @return The extracted text content
     */
    private static String extractTextContent(JsonObject jsonResponse) {
        StringBuilder contentBuilder = new StringBuilder();
        
        try {
            if (jsonResponse.has("content") && jsonResponse.get("content").isJsonArray()) {
                JsonArray contentArray = jsonResponse.get("content").getAsJsonArray();
                
                for (JsonElement element : contentArray) {
                    if (element.isJsonObject()) {
                        JsonObject contentObj = element.getAsJsonObject();
                        
                        if (contentObj.has("type") && contentObj.get("type").getAsString().equals("text")) {
                            if (contentObj.has("text")) {
                                String text = contentObj.get("text").getAsString();
                                contentBuilder.append(text).append("\n");
                            }
                        }
                    }
                }
            }
            
            return contentBuilder.toString();
        } catch (Exception e) {
            ClaudeMod.log("Error extracting text content: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Count the number of actual commands in an MCS file
     * 
     * @param filePath The path to the MCS file
     * @return The number of commands (excluding comments and empty lines)
     */
    private static int countCommands(String filePath) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            List<String> lines = java.nio.file.Files.readAllLines(path);
            
            int commandCount = 0;
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    commandCount++;
                }
            }
            
            return commandCount;
        } catch (IOException e) {
            ClaudeMod.log("Error counting commands in MCS file: " + e.getMessage());
            return 0;
        }
    }
}