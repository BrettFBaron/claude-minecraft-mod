package com.brettbaron.claudemod.config;

import com.brettbaron.claudemod.ClaudeMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ClaudeConfig {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("claudemod");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
    
    private static Properties properties = new Properties();
    private static String apiKey = null;
    
    public static void load() {
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            
            // Create default config file if it doesn't exist
            File configFile = CONFIG_FILE.toFile();
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }
            
            // Load config
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
                apiKey = properties.getProperty("api_key", "");
                
                if (apiKey.isEmpty()) {
                    // Check environment variable as a fallback
                    String envApiKey = System.getenv("CLAUDE_API_KEY");
                    if (envApiKey != null && !envApiKey.isEmpty()) {
                        ClaudeMod.log("Using Claude API key from environment variable");
                    } else {
                        ClaudeMod.log("Claude API key not found in config file or environment. Please set it using /claude-key command or set CLAUDE_API_KEY environment variable.");
                    }
                } else {
                    ClaudeMod.log("Loaded Claude API key from config file");
                }
            }
        } catch (IOException e) {
            ClaudeMod.LOGGER.error("Failed to load config", e);
        }
    }
    
    private static void createDefaultConfig(File configFile) throws IOException {
        // Check for environment variables
        String envApiKey = System.getenv("CLAUDE_API_KEY");
        String envModel = System.getenv("CLAUDE_MODEL");
        
        // Use env variable if available, otherwise empty string for API key
        properties.setProperty("api_key", envApiKey != null ? envApiKey : "");
        
        // Use env variable if available, otherwise default model
        properties.setProperty("model", envModel != null ? envModel : "claude-3-5-sonnet-20240620");
        
        // Add a comment explaining environment variables
        properties.setProperty("# NOTE", "You can also set CLAUDE_API_KEY and CLAUDE_MODEL environment variables");
        
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, "Claude Minecraft Mod Configuration");
        }
        
        if (envApiKey != null) {
            ClaudeMod.log("Created config file with API key from environment variable");
        } else {
            ClaudeMod.log("Created default config file at " + configFile.getAbsolutePath() + 
                          " (API key not set - please use /claude-key command or set CLAUDE_API_KEY environment variable)");
        }
    }
    
    public static void saveApiKey(String newApiKey) {
        apiKey = newApiKey;
        properties.setProperty("api_key", newApiKey);
        
        try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
            properties.store(writer, "Claude Minecraft Mod Configuration");
            ClaudeMod.LOGGER.info("Saved API key to config file");
        } catch (IOException e) {
            ClaudeMod.LOGGER.error("Failed to save API key to config file", e);
        }
    }
    
    public static String getApiKey() {
        // Always check environment variable first - this allows for dynamic updates
        String envApiKey = System.getenv("CLAUDE_API_KEY");
        if (envApiKey != null && !envApiKey.isEmpty()) {
            return envApiKey;
        }
        
        // Fall back to the stored API key
        return apiKey;
    }
    
    public static String getModel() {
        // Check environment variable first
        String envModel = System.getenv("CLAUDE_MODEL");
        if (envModel != null && !envModel.isEmpty()) {
            return envModel;
        }
        
        // Fall back to the stored model or default
        return properties.getProperty("model", "claude-3-5-sonnet-20240620");
    }
}