package com.brettbaron.claudemod.mcs;

import com.brettbaron.claudemod.ClaudeMod;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles Minecraft Command Syntax (MCS) file processing and execution
 */
public class McsProcessor {
    // Directory where MCS files will be stored
    private static final String MCS_DIR = "mcs_files";
    
    /**
     * Initialize the MCS processor, creating necessary directories
     */
    public static void initialize() {
        try {
            // Create the MCS files directory if it doesn't exist
            Path mcsPath = Paths.get(MCS_DIR);
            if (!Files.exists(mcsPath)) {
                Files.createDirectories(mcsPath);
                ClaudeMod.log("Created MCS directory: " + mcsPath.toAbsolutePath());
            }
        } catch (IOException e) {
            ClaudeMod.log("Error initializing MCS processor: " + e.getMessage());
        }
    }
    
    /**
     * Save an MCS file with the given content
     * 
     * @param filename The name of the file
     * @param content The MCS content to save
     * @return The full path to the saved file
     * @throws IOException If there's an error saving the file
     */
    public static String saveMcsFile(String filename, String content) throws IOException {
        // Ensure filename has .mcs extension
        if (!filename.endsWith(".mcs")) {
            filename += ".mcs";
        }
        
        // Create file path
        Path filePath = Paths.get(MCS_DIR, filename);
        
        // Write content to file
        Files.write(filePath, content.getBytes());
        
        ClaudeMod.log("Saved MCS file: " + filePath.toAbsolutePath());
        return filePath.toAbsolutePath().toString();
    }
    
    /**
     * Execute an MCS file line by line in the Minecraft world
     * 
     * @param filePath The path to the MCS file
     * @param source The server command source
     * @return CompletableFuture that completes when all commands are executed
     */
    public static CompletableFuture<Integer> executeMcsFile(String filePath, ServerCommandSource source) {
        ClaudeMod.log("Executing MCS file: " + filePath);
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Run in a separate thread to avoid blocking the main thread
        Thread executionThread = new Thread(() -> {
            try {
                List<String> commands = Files.readAllLines(Paths.get(filePath));
                int totalCommands = commands.size();
                int executedCommands = 0;
                int skippedCommands = 0;
                
                source.sendFeedback(() -> Text.literal("Starting execution of " + totalCommands + " commands..."), false);
                
                for (int i = 0; i < commands.size(); i++) {
                    String command = commands.get(i).trim();
                    
                    // Skip empty lines and comments
                    if (command.isEmpty() || command.startsWith("#")) {
                        skippedCommands++;
                        continue;
                    }
                    
                    // Execute the command
                    final String finalCommand = command;
                    try {
                        // Remove leading slash if present
                        if (command.startsWith("/")) {
                            command = command.substring(1);
                        }
                        
                        source.getServer().getCommandManager().executeWithPrefix(source, command);
                        executedCommands++;
                        
                        // Log progress periodically
                        if (executedCommands % 100 == 0 || executedCommands == totalCommands) {
                            final int progress = executedCommands;
                            final int total = totalCommands - skippedCommands;
                            source.sendFeedback(() -> 
                                Text.literal(String.format("Progress: %d/%d commands executed (%.1f%%)", 
                                    progress, total, (progress * 100.0f / total))), false);
                        }
                        
                        // Small delay to prevent server overload
                        Thread.sleep(5); 
                    } catch (Exception e) {
                        ClaudeMod.log("Error executing command '" + finalCommand + "': " + e.getMessage());
                    }
                }
                
                // Send final feedback
                final int finalExecuted = executedCommands;
                final int finalSkipped = skippedCommands;
                source.sendFeedback(() -> 
                    Text.literal(String.format("Execution complete: %d commands executed, %d lines skipped", 
                        finalExecuted, finalSkipped)), false);
                
                future.complete(executedCommands);
            } catch (Exception e) {
                ClaudeMod.log("Error executing MCS file: " + e.getMessage());
                e.printStackTrace();
                source.sendFeedback(() -> Text.literal("Error executing MCS file: " + e.getMessage()), false);
                future.completeExceptionally(e);
            }
        });
        
        executionThread.setName("MCS-Execution-Thread");
        executionThread.start();
        
        return future;
    }
    
    /**
     * Creates an MCS file from a Claude-generated response
     * 
     * @param response The Claude API response
     * @param buildName Name for the build/file
     * @return Path to the created MCS file
     * @throws IOException If there's an error creating the file
     */
    public static String createMcsFromResponse(String response, String buildName) throws IOException {
        ClaudeMod.log("Creating MCS file from Claude response");
        
        // Extract MCS content from the response
        String mcsContent = extractMcsContent(response);
        
        if (mcsContent == null || mcsContent.isEmpty()) {
            throw new IOException("No MCS content found in Claude's response");
        }
        
        // Sanitize build name for filename
        String filename = buildName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        
        // Save the MCS file
        return saveMcsFile(filename, mcsContent);
    }
    
    /**
     * Extract MCS content from Claude's response
     * 
     * @param response The Claude API response
     * @return The extracted MCS content or null if not found
     */
    private static String extractMcsContent(String response) {
        // Try to find content between code blocks
        String[] codeBlockPatterns = {
            "```mcs\\s*([\\s\\S]*?)\\s*```",
            "```minecraft\\s*([\\s\\S]*?)\\s*```",
            "```([\\s\\S]*?)\\s*```" // Fallback to any code block
        };
        
        for (String pattern : codeBlockPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(response);
            
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        
        // If no code blocks found, try to extract commands directly
        List<String> commands = new ArrayList<>();
        for (String line : response.split("\\n")) {
            line = line.trim();
            // Look for command-like patterns
            if (line.startsWith("/") && !line.contains(" ") && line.length() > 1) {
                commands.add(line);
            }
            else if (line.startsWith("/fill ") || 
                     line.startsWith("/setblock ") || 
                     line.startsWith("/clone ") ||
                     line.startsWith("/execute ")) {
                commands.add(line);
            }
        }
        
        if (!commands.isEmpty()) {
            return String.join("\n", commands);
        }
        
        // If nothing was found, return the entire response as a comment
        return "# No structured MCS content found. Raw response:\n# " + 
               response.replaceAll("\\n", "\n# ");
    }
}