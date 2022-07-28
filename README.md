# Ace of Shades 2
A simple 3D voxel-based shooter inspired by Ace of Spades. With some basic weapons and tools, fight against players in other teams!

![screenshot](/design/gameplay_screenshot.png?raw=true "Test")

## Quick-Start Guide
_Read this guide to get started and join a server in just a minute or two!_

1. Make sure you've got at least Java 17 installed. You can get it [here](https://adoptium.net/temurin/releases).
2. Download the `aos2-client` JAR file from the [releases page](https://github.com/andrewlalis/ace-of-shades-2/releases) that's compatible with your system.
3. Run the game by double-clicking the JAR file, or entering `java -jar <jarfile>` in a terminal. This should generate a `client.yaml` file.
4. Set the `serverHost`, `serverPort`, and `username` properties accordingly for the server you want to join.
5. Run the game again to join the server and start playing!

### Controls
- `WASD` to move, `SPACE` to jump, `LEFT-CONTROL` to crouch.
- `LEFT-CLICK` to use your held item (shoot, destroy blocks, etc.).
- `RIGHT-CLICK` to interact with things using your held item (place blocks, etc.).
- `R` to reload.
- `T` to chat. Press `ENTER` to send the chat.
- `ESCAPE` to exit the game.
- Numbers are used for selecting different items.
- `F3` toggles showing debug info.

## Setting up a Server
Setting up a server is quite easy. Just go to the [releases page](https://github.com/andrewlalis/ace-of-shades-2/releases) and download the latest `aos2-server` JAR file, and run it. It'll create a `server.yaml` configuration file if you don't provide one.

## Configuration
Both the client and server use a similar style of YAML-based configuration, where upon booting up, the program will look for a configuration file in the current working directory with one of the following names: `configuration`, `config`, `cfg`, ending in either `.yaml` or `.yml`. Alternatively, you can provide the path to a configuration file at a different location via a single command-line argument. For example:
```bash
java -jar server.jar /path/to/my/custom/config.yaml
```
If no configuration file is found, and none is explicitly provided, then a set of default configuration options is loaded.
