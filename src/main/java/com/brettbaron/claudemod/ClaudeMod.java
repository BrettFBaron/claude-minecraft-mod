package com.brettbaron.claudemod;

import com.brettbaron.claudemod.command.ApiKeyCommand;
import com.brettbaron.claudemod.command.ClaudeCommand;
import com.brettbaron.claudemod.config.ClaudeConfig;
import com.brettbaron.claudemod.mcs.McsProcessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClaudeMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("claudemod");
    
    // Custom file logging to desktop
    private static FileWriter logFileWriter;
    public static final String MOD_ID = "claudemod";

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Claude Minecraft Mod initializing");
		System.out.println("Claude Minecraft Mod initializing");
		
		// Set up custom desktop logging
		try {
			// Create log directory if it doesn't exist
			File logDir = new File(System.getProperty("user.home") + "/Desktop/minecraft-logs");
			if (!logDir.exists()) {
				logDir.mkdirs();
			}
			
			// Create log file with timestamp
			String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			File logFile = new File(logDir, "claude-mod-" + timestamp + ".log");
			logFileWriter = new FileWriter(logFile, true);
			log("Claude Minecraft Mod initializing - Log file created");
		} catch (IOException e) {
			LOGGER.error("Failed to create custom log file", e);
			System.err.println("Failed to create custom log file: " + e.getMessage());
		}
		
		// Load configuration
		ClaudeConfig.load();
		
		// Initialize MCS processor
		try {
			McsProcessor.initialize();
			log("MCS Processor initialized");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize MCS processor", e);
			System.err.println("Failed to initialize MCS processor: " + e.getMessage());
		}
		
		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ClaudeCommand.register(dispatcher);
			ApiKeyCommand.register(dispatcher);
		});
		
		LOGGER.info("Claude Minecraft Mod initialized successfully");
		log("Claude Minecraft Mod initialized successfully");
	}
	
	/**
	 * Custom logging to both standard logger and our desktop file
	 */
	public static void log(String message) {
		LOGGER.info(message);
		System.out.println("CLAUDE MOD: " + message);
		
		// Also write to our custom log file
		if (logFileWriter != null) {
			try {
				String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				logFileWriter.write("[" + timestamp + "] " + message + "\n");
				logFileWriter.flush();
			} catch (IOException e) {
				LOGGER.error("Failed to write to custom log file", e);
				System.err.println("Failed to write to custom log file: " + e.getMessage());
			}
		}
	}
}