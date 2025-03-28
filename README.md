# Claude Minecraft Mod

A Minecraft mod that uses Claude AI to build structures in-game based on natural language prompts.

## Features

- Use natural language to describe buildings and have them constructed in the game
- Generates efficient Minecraft Command Syntax (MCS) files for building
- Leverages Claude's understanding of Minecraft commands like /fill, /setblock, and /clone
- Creates complex structures with a single command
- Saves all builds as reusable MCS files

## Requirements

- Minecraft 1.20.6
- Fabric Loader 0.15.10+
- Fabric API
- Java 21+
- Claude API key

## Installation

1. Install [Fabric](https://fabricmc.net/use/) for Minecraft 1.20.6
2. Download the mod JAR file from the releases page
3. Place the JAR file in your Minecraft mods folder
4. Set up your Claude API key (see below)
5. Launch Minecraft with Fabric

## Setting up your Claude API Key

This mod requires a Claude API key to function. There are three ways to provide your API key:

### Option 1: Environment Variables (Recommended)

Set the following environment variables:

```
CLAUDE_API_KEY=your-api-key-here
CLAUDE_MODEL=claude-3-7-sonnet-20240307  # Optional, defaults to claude-3-5-sonnet-20240620
```

#### macOS/Linux:
Add to your shell configuration file (`.bashrc`, `.zshrc`, etc.):
```bash
export CLAUDE_API_KEY="your-api-key-here"
export CLAUDE_MODEL="claude-3-7-sonnet-20240307"  # Optional
```

#### Windows:
Set via System Properties → Advanced → Environment Variables

### Option 2: In-Game Command

Use the following command in-game:
```
/claude-key your-api-key-here
```

### Option 3: Config File

Edit the config file located at:
`.minecraft/config/claudemod/config.properties`

This file is automatically created when you first run the mod. It's also automatically updated when you use the `/claude-key` command in-game. If you edit this file manually, you'll need to restart Minecraft for changes to take effect.

## Usage

Once installed, use the `/claude` command in-game followed by your building request:

```
/claude build a medieval castle with four towers
/claude create a modern house with a swimming pool
/claude make a small wooden cabin with a chimney
```

The mod will send your request to Claude AI, which will generate Minecraft commands to create the requested structure. These commands are saved as an MCS file and then executed in the game.

## How It Works

1. Your building request is sent to Claude AI with specialized prompting
2. Claude generates a series of Minecraft commands (/fill, /setblock, etc.)
3. The mod saves these commands as an MCS file in the `mcs_files` directory
4. Commands are executed sequentially to build your structure
5. Saved MCS files can be reused or shared

## Commands

- `/claude <prompt>` - Build a structure based on your description
- `/claude-key <api-key>` - Set your Claude API key

## Security Note

To keep your API key secure:
- Use environment variables when possible
- Never share your config files or API key
- The mod will not hardcode or expose your API key
- If you're forking or cloning this repository:
  - The config file at `.minecraft/config/claudemod/config.properties` stores your API key when using the `/claude-key` command
  - Make sure this file contains only the placeholder "YOUR_API_KEY_HERE" before pushing to GitHub
  - Consider adding this file to your .gitignore

## License

CC0-1.0 license. Feel free to learn from it and incorporate it in your own projects.
