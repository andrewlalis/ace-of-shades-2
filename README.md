# ace-of-shades-2
A simple 3D voxel-based shooter inspired by Ace of Spades.

## Configuration
Both the client and server use a similar style of YAML-based configuration, where upon booting up, the program will look for a configuration file in the current working directory with one of the following names: `configuration`, `config`, `cfg`, ending in either `.yaml` or `.yml`. Alternatively, you can provide the path to a configuration file at a different location via a single command-line argument. For example:
```bash
java -jar server.jar /path/to/my/custom/config.yaml
```
If no configuration file is found, and none is explicitly provided, then a set of default configuration options is loaded.

## Running the Game
Ace of Shades 2 uses Java 17. You'll need to install that first (or any later version), if you don't have it already.

To run the client, go to the [releases](https://github.com/andrewlalis/ace-of-shades-2/releases) page and download the `aos2-client` file that corresponds to your system. You'll need to make a configuration file in the same directory as the JAR file, something like this:
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
To connect to a particular server, you'll need to update this config file and then you can start the game with `java -jar <jarfile>` or by double-clicking on it.
