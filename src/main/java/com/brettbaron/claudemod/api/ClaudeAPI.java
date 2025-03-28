package com.brettbaron.claudemod.api;

import com.brettbaron.claudemod.ClaudeMod;
import com.brettbaron.claudemod.config.ClaudeConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ClaudeAPI {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final Gson gson = new Gson();
    
    // The Claude tool schema for block placement
    private static final String TOOL_SCHEMA = """
        {
          "tools": [{
            "name": "generate_mcs",
            "description": "Generate Minecraft Command Syntax (MCS) for building structures",
            "input_schema": {
              "type": "object",
              "properties": {
                "commands": {
                  "type": "string",
                  "description": "A string containing multiple Minecraft commands (one per line) to build the structure. Can include /fill, /setblock, /clone, etc."
                }
              },
              "required": ["commands"]
            }
          }]
        }
    """;

    public static String sendRequest(String prompt) throws IOException {
        try {
            String apiKey = ClaudeConfig.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IOException("Claude API key not set. Please use the /claude-key command to set your API key.");
            }

            // Construct the request to Claude API
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", ClaudeConfig.getModel());
            requestBody.addProperty("max_tokens", 8192);
            requestBody.addProperty("system", "You are an expert Minecraft architect and builder who specializes in creating efficient command sequences for building structures. " +
                    "You understand Minecraft's commands like /fill, /setblock, /clone, and /execute, and know how to use them efficiently to create complex builds. " +
                    "You follow architectural principles like proportion, balance, and aesthetic design. " +
                    "When asked to build something, you create a sequence of Minecraft commands that, when executed in order, will create the requested structure. " +
                    "Your output should be a complete set of Minecraft commands in a code block, ready for execution in-game.");
            
            // Create messages array properly
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", 
                "# MINECRAFT COMMAND GENERATOR GUIDE\n\n" +
                
                "## COMMAND TYPES\n" +
                "Use these powerful commands efficiently to build structures:\n\n" +
                
                "### FILL COMMAND\n" +
                "- Format: `/fill x1 y1 z1 x2 y2 z2 block [data] [options]`\n" +
                "- Use for large areas like walls, floors, roofs\n" +
                "- Limit to areas of 32,768 blocks or less\n" +
                "- Example: `/fill ~0 ~0 ~0 ~10 ~0 ~10 minecraft:stone`\n\n" +
                
                "### SETBLOCK COMMAND\n" +
                "- Format: `/setblock x y z block [data] [options]`\n" +
                "- Use for single blocks or precise placement\n" +
                "- Great for blocks with specific states/properties\n" +
                "- Example: `/setblock ~5 ~1 ~5 minecraft:oak_door[half=lower,facing=east]`\n\n" +
                
                "### CLONE COMMAND\n" +
                "- Format: `/clone x1 y1 z1 x2 y2 z2 x y z [options]`\n" +
                "- Use for repeating patterns or symmetry\n" +
                "- Can mirror structures using carefully chosen coordinates\n" +
                "- Example: `/clone ~0 ~0 ~0 ~5 ~5 ~5 ~10 ~0 ~0`\n\n" +
                
                "### EXECUTE COMMAND\n" +
                "- Format: `/execute ... run command`\n" +
                "- Use for complex conditional building\n" +
                "- Can replace only specific blocks\n" +
                "- Example: `/execute if block ~0 ~-1 ~0 minecraft:stone run setblock ~0 ~0 ~0 minecraft:grass_block`\n\n" +
                
                "## EFFICIENCY TIPS\n" +
                
                "### OPTIMIZE COMMAND COUNT\n" +
                "- Use `/fill` for large areas rather than many `/setblock` commands\n" +
                "- Build in logical order: foundation → walls → roof → details\n" +
                "- Use relative coordinates (~ ~ ~) based on player position\n" +
                "- When appropriate, use `/clone` to copy repeated elements\n\n" +
                
                "### TECHNICAL LIMITATIONS\n" +
                "- Maximum 32,768 blocks per `/fill` command\n" +
                "- Commands with relative coordinates (~) are based on player position\n" +
                "- Use comments (lines starting with #) to organize sections\n" +
                "- Some blocks require certain block states (doors, stairs, etc.)\n\n" +
                
                "## AVAILABLE BLOCKS\n" +
                "All Minecraft blocks can be used with these formats:\n" +
                "- Basic blocks: `minecraft:stone`, `minecraft:oak_planks`\n" +
                "- Blocks with states: `minecraft:oak_stairs[facing=north,half=bottom]`\n\n" +
                
                "## OUTPUT FORMAT\n" +
                "Your response should include ONLY a code block containing commands:\n" +
                "```\n" +
                "# Foundation\n" +
                "/fill ~0 ~0 ~0 ~10 ~0 ~10 minecraft:stone\n" +
                "# Walls\n" +
                "/fill ~0 ~1 ~0 ~10 ~4 ~0 minecraft:oak_planks\n" +
                "# Etc...\n" +
                "```\n\n" +
                
                "## YOUR TASK\n" +
                "Create a comprehensive set of Minecraft commands that will build: " + prompt);
            
            com.google.gson.JsonArray messagesArray = new com.google.gson.JsonArray();
            messagesArray.add(userMessage);
            
            // Add messages as a JsonArray
            requestBody.add("messages", messagesArray);
            
            // Add the tool schema
            JsonObject toolsJson = gson.fromJson(TOOL_SCHEMA, JsonObject.class);
            requestBody.add("tools", toolsJson.get("tools"));
            
            // Log the request details
            ClaudeMod.log("Making Claude API request to: " + API_URL);
            ClaudeMod.log("Request body: " + requestBody.toString());
            
            // Create HTTP client
            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
            
            // Create request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(1))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            ClaudeMod.log("Sending API request with headers: x-api-key=" + apiKey.substring(0, 5) + "..., anthropic-version=2023-06-01");
            
            // Send request and get response
            ClaudeMod.log("Executing HTTP request...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            ClaudeMod.log("Received response code: " + statusCode);
            
            // Check if request was successful
            if (statusCode < 200 || statusCode >= 300) {
                String errorBody = response.body();
                ClaudeMod.log("API error response: " + errorBody);
                throw new IOException("Unexpected response code: " + statusCode + " - " + errorBody);
            }
            
            String responseBody = response.body();
            
            if (responseBody == null || responseBody.isEmpty()) {
                ClaudeMod.log("API returned empty response");
                throw new IOException("API returned empty response");
            }
            
            ClaudeMod.log("Claude API response received with length: " + responseBody.length());
            ClaudeMod.log("API RESPONSE PREVIEW: " + responseBody.substring(0, Math.min(responseBody.length(), 100)) + "...");
            
            return responseBody;
        } catch (Exception e) {
            ClaudeMod.log("Error calling Claude API: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Error calling Claude API", e);
        }
    }
}