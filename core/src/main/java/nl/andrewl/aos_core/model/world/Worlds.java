package nl.andrewl.aos_core.model.world;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Collections;

/**
 * Simple container for a bunch of static methods for creating pre-made worlds
 * for when you don't want to spend time on that yourself.
 */
public final class Worlds {
	private Worlds() {}

	/**
	 * A world consisting of a single chunk, with blocks from y = 0 to y = 8.
	 * @return The world.
	 */
	public static World smallCube() {
		Chunk chunk = new Chunk(0, 0, 0);
		for (int x = 0; x < Chunk.SIZE; x++) {
			for (int z = 0; z < Chunk.SIZE; z++) {
				for (int y = 0; y < Chunk.SIZE / 2; y++) {
					chunk.setBlockAt(x, y, z, (byte) 40);
				}
			}
		}
		return new World(ColorPalette.rainbow(), Collections.singleton(chunk));
	}

	/**
	 * A 3x3 chunk world consisting of various structures and areas that are
	 * ideal for testing the game.
	 * @return The world.
	 */
	public static World testingWorld() {
		ColorPalette palette = new ColorPalette();
		palette.setColor((byte) 1, 0.610f, 0.604f, 0.494f);// light brown
		palette.setColor((byte) 9, 0.610f, 0.604f, 0.2f);// light brown
		palette.setColor((byte) 2, 1, 1, 1);// white
		palette.setColor((byte) 3, 0, 0, 0);// black

		palette.setColor((byte) 4, 1, 0, 0);// red
		palette.setColor((byte) 5, 0, 1, 0);// green
		palette.setColor((byte) 6, 0, 0, 1);// blue
		palette.setColor((byte) 7, 1, 1, 0);// yellow
		palette.setColor((byte) 8, 1, 0, 0);// magenta

		World world = new World(palette);
		for (int x = -1; x < 2; x++) {
			for (int z = -1; z < 2; z++) {
				for (int y = -1; y < 2; y++) {
					world.addChunk(new Chunk(x, y, z));
				}
			}
		}
		Vector3i min = new Vector3i(-1 * Chunk.SIZE);
		Vector3i max = new Vector3i(2 * Chunk.SIZE - 1);
		int groundLevel = 0;
		for (int x = min.x; x <= max.x; x++) {
			for (int z = min.z; z <= max.z; z++) {
				for (int y = min.y; y < groundLevel; y++) {
					byte color;
					if (x % 2 == 0 && z % 2 == 0) {
						color = 1;
					} else {
						color = 9;
					}
					world.setBlockAt(x, y, z, color);
				}
			}
		}

		// -Z axis
		for (int z = min.z; z < 0; z++) {
			world.setBlockAt(0, -1, z, (byte) 4);
		}
		// +Z axis
		for (int z = 0; z <= max.z; z++) {
			world.setBlockAt(0, -1, z, (byte) 6);
		}
		// -X axis
		for (int x = min.x; x < 0; x++) {
			world.setBlockAt(x, -1, 0, (byte) 5);
		}
		// +X axis
		for (int x = 0; x <= max.x; x++) {
			world.setBlockAt(x, -1, 0, (byte) 7);
		}
		// Draw a '+' in the + side of the world.
		world.setBlockAt(10, -1, 10, (byte) 3);
		world.setBlockAt(11, -1, 10, (byte) 3);
		world.setBlockAt(12, -1, 10, (byte) 3);
		world.setBlockAt(9, -1, 10, (byte) 3);
		world.setBlockAt(8, -1, 10, (byte) 3);
		world.setBlockAt(10, -1, 9, (byte) 3);
		world.setBlockAt(10, -1, 8, (byte) 3);
		world.setBlockAt(10, -1, 11, (byte) 3);
		world.setBlockAt(10, -1, 12, (byte) 3);
		// Draw a '-' in the - side of the world.
		world.setBlockAt(-7, -1, -8, (byte) 3);
		world.setBlockAt(-8, -1, -8, (byte) 3);
		world.setBlockAt(-9, -1, -8, (byte) 3);
		world.setBlockAt(-10, -1, -8, (byte) 3);
		world.setBlockAt(-11, -1, -8, (byte) 3);

		// Draw a '+' shaped wall.
		for (int x = 16; x < 26; x++) {
			world.setBlockAt(x, 0, 16, (byte) 1);
			world.setBlockAt(x, 1, 16, (byte) 1);
			world.setBlockAt(x, 2, 16, (byte) 1);
		}
		for (int z = 16; z < 26; z++) {
			world.setBlockAt(16, 0, z, (byte) 1);
			world.setBlockAt(16, 1, z, (byte) 1);
			world.setBlockAt(16, 2, z, (byte) 1);
		}
		// Add a small staircase.
		world.setBlockAt(14, 0, 20, (byte) 1);
		world.setBlockAt(14, 0, 21, (byte) 1);
		world.setBlockAt(14, 0, 22, (byte) 1);
		world.setBlockAt(15, 1, 20, (byte) 1);
		world.setBlockAt(15, 1, 21, (byte) 1);
		world.setBlockAt(15, 1, 22, (byte) 1);
		// Add a small floor area.
		for (int x = 17; x < 26; x++) {
			for (int z = 17; z < 26; z++) {
				world.setBlockAt(x, 3, z, (byte) 1);
			}
		}

		world.setSpawnPoint("A", new Vector3f(0.5f, 0f, 0.5f));
		world.setSpawnPoint("B", new Vector3f(20.5f, 0f, 20.5f));

		return world;
	}
}
