package com.brettbaron.claudemod.client;

import com.brettbaron.claudemod.ClaudeMod;
import net.fabricmc.api.ClientModInitializer;

public class ClaudeModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClaudeMod.LOGGER.info("Claude Minecraft Mod client initialized");
	}
}