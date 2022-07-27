package nl.andrewl.aos_core.model.world;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.*;
import java.util.Collections;
import java.util.Random;

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
		World world = new World(ColorPalette.rainbow(), Collections.singleton(chunk));
		world.setSpawnPoint("A", new Vector3f(Chunk.SIZE / 2f, Chunk.SIZE / 2f, Chunk.SIZE / 2f));
		return world;
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

	public static World flatWorld() {
		World world = new World(ColorPalette.rainbow());
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				for (int z = 0; z < 12; z++) {
					world.addChunk(new Chunk(x, y, z));
				}
			}
		}
		world.setBlocksAt(0, 0, 0, 5 * Chunk.SIZE, 2 * Chunk.SIZE, 12 * Chunk.SIZE, (byte) 80);

		world.setSpawnPoint("A", new Vector3f(5, 34, 5));
		world.setSpawnPoint("B", new Vector3f(5 * Chunk.SIZE - 5, 34, 12 * Chunk.SIZE - 5));

		return world;
	}

	/**
	 * A square arena for up to 4 teams, with spawn points A, B, C, and D.
	 * @return The world.
	 */
	public static World arena() {
		World world = new World();
		ColorPalette palette = new ColorPalette();
		palette.setColor((byte) 1, Color.BLACK);
		palette.setColor((byte) 2, Color.WHITE);
		palette.setColor((byte) 3, Color.GRAY);
		palette.setColor((byte) 4, Color.DARK_GRAY);
		palette.setColor((byte) 5, new Color(79, 58, 0));
		palette.setColor((byte) 6, new Color(133, 118, 78));
		palette.setColor((byte) 7, new Color(69, 59, 32));
		palette.setColor((byte) 8, new Color(38, 77, 30));
		palette.setColor((byte) 9, new Color(4, 107, 40));
		palette.setColor((byte) 10, Color.RED.darker());
		palette.setColor((byte) 11, Color.GREEN.darker());
		palette.setColor((byte) 12, Color.BLUE.darker());
		world.setPalette(palette);
		Random rand = new Random(1L);
		for (int cx = 0; cx < 9; cx++) {
			for (int cz = 0; cz < 9; cz++) {
				for (int cy = 0; cy < 5; cy++) {
					world.addChunk(new Chunk(cx, cy, cz));
				}
			}
		}
		int surface = 3 * Chunk.SIZE;
		for (int x = world.getMinX(); x < world.getMaxX(); x++) {
			for (int z = world.getMinZ(); z < world.getMaxZ(); z++) {
				for (int y = 0; y < surface - 1; y++) {
					world.setBlockAt(x, y, z, (byte) rand.nextInt(5, 8));
				}
				world.setBlockAt(x, surface - 1, z, (byte) rand.nextInt(8, 10));
			}
		}

		world.setSpawnPoint("A", new Vector3f(5.5f, surface, 5.5f));
		world.setSpawnPoint("C", new Vector3f(world.getMaxX() - 5.5f, surface, 5.5f));
		world.setSpawnPoint("D", new Vector3f(5.5f, surface, world.getMaxZ() - 5.5f));
		world.setSpawnPoint("B", new Vector3f(world.getMaxX() - 5.5f, surface, world.getMaxZ() - 5.5f));

		return world;
	}
}
