# Ace of Shades 2
A simple 3D voxel-based shooter inspired by Ace of Spades. With some basic weapons and tools, fight against players in other teams!

![screenshot](/design/gameplay_screenshot.png?raw=true "Test")

## Quick-Start Guide
_Read this guide to get started and join a server in just a minute or two!_

1. Make sure you've got at least Java 17 installed. You can get it [here](https://adoptium.net/temurin/releases).
2. Download the `aos2-client` JAR file from the [releases page](https://github.com/andrewlalis/ace-of-shades-2/releases) that's compatible with your system.
3. Create a file named `config.yaml` in the same directory as the JAR file, and place the following text in it:
```yaml
serverHost: localhost
serverPort: 25565
username: myUsername
input:
  mouseSensitivity: 0.005
display:
  fullscreen: true
  captureCursor: true
  fov: 80
```
4. Set the `serverHost`, `serverPort`, and `username` properties accordingly for the server you want to join.
5. Run the game by double-clicking the `aos2-client` JAR file, or enter `java -jar aos2-client-{version}.jar` in a terminal.

## Setting up a Server
Setting up a server is quite easy. Just go to the [releases page](https://github.com/andrewlalis/ace-of-shades-2/releases) and download the latest `aos2-server` JAR file. Similar to the client, it's best if you provide a `config.yaml` file to the server, in the same directory. The following snippet shows the structure and default values of a server's configuration.
```yaml
port: 25565
connectionBacklog: 5
ticksPerSecond: 20.0
physics:
  gravity: 29.43
  walkingSpeed: 4
  crouchingSpeed: 1.5
  sprintingSpeed: 9
  movementAcceleration: 2
  movementDeceleration: 1
  jumpVerticalSpeed: 8
actions:
  blockBreakCooldown: 0.25
  blockPlaceCooldown: 0.1
  blockBreakReach: 5
  blockPlaceReach: 5
  blockBulletDamageResistance: 3
  blockBulletDamageCooldown: 10
  resupplyCooldown: 30
  resupplyRadius: 3
  teamSpawnProtection: 10
  movementAccuracyDecreaseFactor: 0.01
  friendlyFire: false
```

## Configuration
Both the client and server use a similar style of YAML-based configuration, where upon booting up, the program will look for a configuration file in the current working directory with one of the following names: `configuration`, `config`, `cfg`, ending in either `.yaml` or `.yml`. Alternatively, you can provide the path to a configuration file at a different location via a single command-line argument. For example:
```bash
java -jar server.jar /path/to/my/custom/config.yaml
```
If no configuration file is found, and none is explicitly provided, then a set of default configuration options is loaded.
