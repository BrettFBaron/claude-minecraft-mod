# Claude Minecraft Mod

A Minecraft mod that uses Claude AI to build structures in-game based on natural language prompts.

## Features

- Use natural language to describe buildings and have them constructed in the game
- Sophisticated architectural prompting system that guides Claude in creating high-quality structures
- Multi-phase building process for complex structures
- Custom block placement API

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

## Usage

Once installed, use the `/claude` command in-game followed by your building request:

```
/claude build a medieval castle with four towers
/claude create a modern house with a swimming pool
/claude make a small wooden cabin with a chimney
```

The mod will send your request to Claude AI, which will then generate and place blocks to create the requested structure.

## Commands

- `/claude <prompt>` - Build a structure based on your description
- `/claude-key <api-key>` - Set your Claude API key

## Security Note

To keep your API key secure:
- Use environment variables when possible
- Never share your config files or API key
- The mod will not hardcode or expose your API key

## License

CC0-1.0 license. Feel free to learn from it and incorporate it in your own projects.
