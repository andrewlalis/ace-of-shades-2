# ace-of-shades-2
A simple 3D voxel-based shooter inspired by Ace of Spades.

## Configuration
Both the client and server use a similar style of YAML-based configuration, where upon booting up, the program will look for a configuration file in the current working directory with one of the following names: `configuration`, `config`, `cfg`, ending in either `.yaml` or `.yml`. Alternatively, you can provide the path to a configuration file at a different location via a single command-line argument. For example:
```bash
java -jar server.jar /path/to/my/custom/config.yaml
```
If no configuration file is found, and none is explicitly provided, then a set of default configuration options is loaded.
