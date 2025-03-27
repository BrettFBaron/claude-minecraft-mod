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
            "name": "place_blocks",
            "description": "Place blocks in the Minecraft world at specific coordinates",
            "input_schema": {
              "type": "object",
              "properties": {
                "blocks": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "block": {"type": "string", "description": "The Minecraft block type, e.g. minecraft:quartz_block"},
                      "x": {"type": "integer", "description": "X coordinate"},
                      "y": {"type": "integer", "description": "Y coordinate"},
                      "z": {"type": "integer", "description": "Z coordinate"}
                    },
                    "required": ["block", "x", "y", "z"]
                  }
                }
              },
              "required": ["blocks"]
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
            requestBody.addProperty("system", "You are an expert Minecraft architect and builder who creates beautiful, functional structures. " +
                    "When designing buildings, you follow architectural principles including proportion, balance, structural integrity, " +
                    "and aesthetic harmony. You understand Minecraft's unique building constraints and opportunities. " +
                    "Your designs use depth, texture variation, and appropriate block palettes to create visually impressive buildings.");
            
            // Create messages array properly
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", 
                "# MINECRAFT BUILDING EXPERT GUIDE\n\n" +
                
                "## DESIGN PROCESS\n" +
                "Follow this structured approach to create high-quality Minecraft structures:\n\n" +
                
                "### PHASE 1: CONCEPTUALIZATION\n" +
                "1. Analyze the building request carefully (style, purpose, size)\n" +
                "2. Envision the complete structure in 3D space\n" +
                "3. Determine appropriate dimensions (use odd numbers like 5, 7, 9 for width/length)\n" +
                "4. Choose a block palette of 3-5 complementary materials\n" +
                "5. Sketch floor plan relative to player's position\n\n" +
                
                "### PHASE 2: ARCHITECTURAL PLANNING\n" +
                "6. Design the foundation (1-2 blocks deep, extending 1 block past walls)\n" +
                "7. Plan walls with corners, framing, and variation in depth\n" +
                "8. Design appropriate roof style (gabled, hipped, flat) with overhang\n" +
                "9. Place windows and doors at proper intervals (odd-numbered spacing)\n" +
                "10. Add structural elements (columns, beams) where needed\n\n" +
                
                "### PHASE 3: EXECUTION WITH BLOCK API\n" +
                "11. Use the 'place_blocks' tool with proper block IDs\n" +
                "12. Build in logical order: foundation → frame → walls → roof → details\n" +
                "13. Test structural integrity throughout (no floating elements)\n" +
                "14. Apply architectural principles for aesthetics\n" +
                "15. Add final detailing and landscaping touches\n\n" +
                
                "## AVAILABLE BLOCK RESOURCES\n" +
                "Use these common Minecraft blocks (always prefix with 'minecraft:'):\n\n" +
                
                "### STONE MATERIALS\n" +
                "- stone, cobblestone, stone_bricks, mossy_stone_bricks\n" +
                "- andesite, diorite, granite (+ polished variants)\n" +
                "- deepslate, tuff, calcite, smooth_basalt\n" +
                "- blackstone, gilded_blackstone\n\n" +
                
                "### WOOD MATERIALS\n" +
                "- oak, spruce, birch, jungle, acacia, dark_oak, crimson, warped\n" +
                "- Available as: _planks, _log, _slab, _stairs, _fence\n" +
                "- Use stripped_[wood]_log for cleaner wood textures\n\n" +
                
                "### DECORATIVE BLOCKS\n" +
                "- Slabs and stairs (for depth and detail)\n" +
                "- Walls (for textured fences and columns)\n" +
                "- Glass, glass_pane (for windows)\n" +
                "- Doors: oak_door, spruce_door, etc.\n" +
                "- Lanterns, torches, glowstone (for lighting)\n" +
                "- flower_pot, bookshelf, barrel (for details)\n\n" +
                
                "### SPECIALTY MATERIALS\n" +
                "- Concrete (16 colors): white_concrete, gray_concrete, etc.\n" +
                "- Terracotta (16 colors): red_terracotta, blue_terracotta, etc.\n" +
                "- Wool (16 colors): yellow_wool, black_wool, etc.\n" +
                "- Copper: copper_block, exposed_copper, weathered_copper, oxidized_copper\n" +
                "- Quartz: quartz_block, quartz_pillar, smooth_quartz\n" +
                "- Rare materials: prismarine, purpur_block, nether_bricks\n\n" +
                
                "## PROPER API USAGE\n" +
                "When using the 'place_blocks' tool to send block placements to Minecraft:\n\n" +
                
                "### BLOCK FORMAT REQUIREMENTS\n" +
                "1. Each block must be properly formatted as:\n" +
                "   ```\n" +
                "   {\n" +
                "     \"block\": \"minecraft:block_name\",\n" +
                "     \"x\": X_COORDINATE_INTEGER,\n" +
                "     \"y\": Y_COORDINATE_INTEGER,\n" +
                "     \"z\": Z_COORDINATE_INTEGER\n" +
                "   }\n" +
                "   ```\n" +
                
                "2. ALWAYS include the 'minecraft:' namespace prefix\n" +
                "3. Use EXACT block IDs (e.g., \"minecraft:oak_planks\" not \"minecraft:oak\")\n" +
                "4. Coordinates must be integers relative to world position\n" +
                "5. Y-coordinate increases upward (build foundation at player level or below)\n" +
                "6. Generate coordinates in a systematic way (loop through x, y, z ranges)\n\n" +
                
                "### TECHNICAL LIMITATIONS\n" +
                "1. Maximum of 5000 blocks per structure\n" +
                "2. Build within 50 blocks of player position when possible\n" +
                "3. Ensure all blocks are connected (no floating parts)\n" +
                "4. Some blocks require support underneath (respect gravity)\n" +
                "5. Consider solid blocks for structure, decorative blocks for details\n\n" +
                
                "## ARCHITECTURAL PRINCIPLES\n" +
                "Apply these fundamental principles to all builds:\n\n" +
                
                "### DEPTH & TEXTURE\n" +
                "- Never build flat walls (use depth variation of at least 1 block)\n" +
                "- Mix complementary blocks in patterns (2-3 types per wall)\n" +
                "- Use stairs, slabs, and walls to create depth and detail\n" +
                "- Frame openings with contrasting or heavier materials\n\n" +
                
                "### PROPORTION & SCALE\n" +
                "- Follow the rule of odds (3, 5, 7, 9 block dimensions)\n" +
                "- Maintain ceiling height of 3-5 blocks for interiors\n" +
                "- Use the golden ratio (1:1.618) for pleasing rectangular shapes\n" +
                "- Scale details appropriately to overall structure size\n\n" +
                
                "### STRUCTURAL INTEGRITY\n" +
                "- Add visible supports under large spans (beams every 5-7 blocks)\n" +
                "- Taper structures as they rise (wider base, narrower top)\n" +
                "- Use heavier materials for lower portions of buildings\n" +
                "- Create logical load-bearing elements (columns, foundations)\n\n" +
                
                "### DETAILING\n" +
                "- Add trim along roof edges and corners\n" +
                "- Create windowsills using slabs or stairs\n" +
                "- Use fence posts and walls for railings and small details\n" +
                "- Incorporate small asymmetries for natural appearance\n" +
                "- Add small 1-block decorative elements for character\n\n" +
                
                "## EXAMPLES OF GOOD BLOCK COMBINATIONS\n" +
                
                "### MEDIEVAL/RUSTIC STYLE\n" +
                "- Foundation: stone, cobblestone\n" +
                "- Walls: oak_planks with spruce_log frame\n" +
                "- Accent: cobblestone, stone_bricks\n" +
                "- Roof: dark_oak_stairs\n" +
                "- Details: lanterns, flower_pots\n\n" +
                
                "### MODERN STYLE\n" +
                "- Foundation: smooth_stone, concrete\n" +
                "- Walls: white_concrete, gray_concrete, glass\n" +
                "- Accent: black_concrete, quartz\n" +
                "- Roof: flat with gray_concrete\n" +
                "- Details: end_rods, iron_bars\n\n" +
                
                "### FANTASY STYLE\n" +
                "- Foundation: deepslate_bricks\n" +
                "- Walls: prismarine with warped_log frame\n" +
                "- Accent: copper_block, gold_block\n" +
                "- Roof: purple_glazed_terracotta\n" +
                "- Details: sea_lanterns, amethyst_block\n\n" +
                
                "## YOUR TASK\n" +
                "Based on this guide, create the following structure: " + prompt);
            
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