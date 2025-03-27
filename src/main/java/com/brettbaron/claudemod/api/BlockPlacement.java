package com.brettbaron.claudemod.api;

import com.brettbaron.claudemod.ClaudeMod;
import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class BlockPlacement {
    private static final Gson gson = new Gson();
    
    public static int processResponse(String apiResponse, ServerCommandSource source) {
        try {
            // Parse the API response
            ClaudeMod.log("Parsing API response");
            ClaudeMod.log("API Response excerpt: " + apiResponse.substring(0, Math.min(apiResponse.length(), 200)) + "...");
            
            JsonObject responseJson = gson.fromJson(apiResponse, JsonObject.class);
            
            // More robust content checking
            if (!responseJson.has("content")) {
                ClaudeMod.LOGGER.error("Invalid API response format: missing content");
                System.out.println("CLAUDE MOD DEBUG - Invalid API response: missing content");
                throw new JsonParseException("Invalid API response format: missing content");
            }
            
            // Check if content is an array
            JsonElement contentElement = responseJson.get("content");
            if (!contentElement.isJsonArray()) {
                ClaudeMod.log("Invalid API response format: content is not an array, it's a " + contentElement.getClass().getSimpleName());
                throw new JsonParseException("Invalid API response format: content is not an array");
            }
            
            // Look for tool calls in the response
            List<JsonObject> toolCalls = new ArrayList<>();
            JsonArray contentArray = contentElement.getAsJsonArray();
            ClaudeMod.log("Content array from response: " + contentArray);
            
            for (JsonElement contentEl : contentArray) {
                // Check if this element is a JsonObject before casting
                if (!contentEl.isJsonObject()) {
                    ClaudeMod.log("Skipping non-object content element: " + contentEl);
                    continue;
                }
                
                JsonObject contentObj = contentEl.getAsJsonObject();
                ClaudeMod.log("Processing content element: " + contentObj);
                
                if (contentObj.has("type") && contentObj.get("type").getAsString().equals("tool_use")) {
                    ClaudeMod.log("Found tool_use content");
                    
                    if (contentObj.has("id") && contentObj.has("name") && contentObj.has("input")) {
                        ClaudeMod.LOGGER.info("Tool call: id=" + contentObj.get("id").getAsString() + 
                                              ", name=" + contentObj.get("name").getAsString());
                        
                        if (contentObj.get("name").getAsString().equals("place_blocks")) {
                            ClaudeMod.LOGGER.info("Found place_blocks tool call");
                            toolCalls.add(contentObj);
                        }
                    }
                }
            }
            
            if (toolCalls.isEmpty()) {
                throw new JsonParseException("No valid place_blocks tool calls found in response");
            }
            
            // Process each tool call to place blocks
            World world = source.getWorld();
            int blocksPlaced = 0;
            
            for (JsonObject toolCall : toolCalls) {
                // Get the input which contains block information
                ClaudeMod.LOGGER.info("Processing tool call: " + toolCall);
                
                // Check if input is present and is a JsonObject
                if (!toolCall.has("input")) {
                    ClaudeMod.LOGGER.error("Tool call missing input field");
                    System.out.println("CLAUDE MOD DEBUG - Tool call missing input field: " + toolCall);
                    continue;
                }
                
                JsonElement inputElement = toolCall.get("input");
                if (!inputElement.isJsonObject()) {
                    ClaudeMod.LOGGER.error("Tool input is not a JSON object: " + inputElement);
                    System.out.println("CLAUDE MOD DEBUG - Tool input is not a JSON object: " + inputElement);
                    continue;
                }
                
                JsonObject input = inputElement.getAsJsonObject();
                ClaudeMod.LOGGER.info("Tool input: " + input);
                System.out.println("CLAUDE MOD DEBUG - Tool input: " + input);
                
                if (!input.has("blocks")) {
                    ClaudeMod.LOGGER.error("No blocks array found in tool input");
                    System.out.println("CLAUDE MOD DEBUG - No blocks array in tool input: " + input);
                    continue; // Skip if no blocks array
                }
                
                // Check if blocks is an array
                JsonElement blocksElement = input.get("blocks");
                if (!blocksElement.isJsonArray()) {
                    ClaudeMod.LOGGER.error("Blocks field is not an array: " + blocksElement);
                    System.out.println("CLAUDE MOD DEBUG - Blocks field is not an array: " + blocksElement);
                    continue;
                }
                
                JsonArray blocksArray = blocksElement.getAsJsonArray();
                ClaudeMod.LOGGER.info("Found blocks array with " + blocksArray.size() + " blocks to place");
                
                for (JsonElement blockElement : blocksArray) {
                    // Check if this element is a JsonObject before casting
                    if (!blockElement.isJsonObject()) {
                        ClaudeMod.LOGGER.warn("Skipping non-object block element: " + blockElement);
                        System.out.println("CLAUDE MOD DEBUG - Skipping non-object block: " + blockElement);
                        continue;
                    }
                    
                    JsonObject blockInfo = blockElement.getAsJsonObject();
                    ClaudeMod.LOGGER.info("Processing block: " + blockInfo);
                    
                    try {
                        // Extract block details
                        String blockName = blockInfo.get("block").getAsString();
                        int x = blockInfo.get("x").getAsInt();
                        int y = blockInfo.get("y").getAsInt();
                        int z = blockInfo.get("z").getAsInt();
                        
                        ClaudeMod.LOGGER.info("Attempting to place: " + blockName + " at (" + x + ", " + y + ", " + z + ")");
                        
                        // Place the block in the world
                        Block block = Registries.BLOCK.get(new Identifier(blockName));
                        if (block == null) {
                            ClaudeMod.LOGGER.error("Invalid block type: " + blockName);
                            continue;
                        }
                        
                        BlockPos pos = new BlockPos(x, y, z);
                        
                        // Set the block state
                        boolean success = world.setBlockState(pos, block.getDefaultState());
                        if (success) {
                            blocksPlaced++;
                            ClaudeMod.LOGGER.info("Successfully placed " + blockName + " at (" + x + ", " + y + ", " + z + ")");
                            
                            // Log every 50 blocks to avoid spam
                            if (blocksPlaced % 50 == 0) {
                                ClaudeMod.LOGGER.info("Placed " + blocksPlaced + " blocks so far");
                                final int progressCount = blocksPlaced;
                                source.sendFeedback(() -> Text.literal("Progress: " + progressCount + " blocks placed..."), false);
                            }
                        } else {
                            ClaudeMod.LOGGER.error("Failed to place " + blockName + " at (" + x + ", " + y + ", " + z + ")");
                        }
                    } catch (Exception e) {
                        ClaudeMod.LOGGER.error("Error processing block: " + blockInfo, e);
                    }
                }
            }
            
            ClaudeMod.log("Successfully placed " + blocksPlaced + " blocks");
            
            // Return the total number of blocks placed
            return blocksPlaced;
            
        } catch (JsonParseException e) {
            ClaudeMod.log("Error parsing Claude API response: " + e.getMessage());
            source.sendFeedback(() -> Text.literal("Error: Could not parse Claude's response - " + e.getMessage()), false);
            return 0;
        } catch (Exception e) {
            ClaudeMod.log("Error placing blocks: " + e.getMessage());
            e.printStackTrace();
            source.sendFeedback(() -> Text.literal("Error placing blocks: " + e.getMessage()), false);
            return 0;
        }
    }
}