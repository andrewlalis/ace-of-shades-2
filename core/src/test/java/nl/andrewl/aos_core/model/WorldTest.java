package nl.andrewl.aos_core.model;

import nl.andrewl.aos_core.Directions;
import nl.andrewl.aos_core.model.world.Chunk;
import nl.andrewl.aos_core.model.world.Hit;
import nl.andrewl.aos_core.model.world.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WorldTest {
	@Test
	public void testGetBlockAt() {
		Chunk chunk = new Chunk(0, 0, 0);
		chunk.setBlockAt(1, 0, 0, (byte) 1);
		World world = new World();
		world.addChunk(chunk);
		assertEquals(1, world.getBlockAt(new Vector3f(1, 0, 0)));
		assertEquals(1, world.getBlockAt(new Vector3f(1.9f, 0, 0)));
		assertEquals(1, world.getBlockAt(new Vector3f(1.5f, 0.7f, 0.3f)));
		assertEquals(0, world.getBlockAt(new Vector3f(2f, 0.7f, 0.3f)));
	}

	@Test
	public void testGetChunkPosAt() {
		assertEquals(new Vector3i(0, 0, 0), World.getChunkPosAt(new Vector3f(0, 0, 0)));
		assertEquals(new Vector3i(0, 0, 0), World.getChunkPosAt(new Vector3f(1, 0, 0)));
		assertEquals(new Vector3i(0, 0, 0), World.getChunkPosAt(new Vector3f(0, 0.5f, 0)));
		assertEquals(new Vector3i(0, 0, 0), World.getChunkPosAt(new Vector3f(Chunk.SIZE - 1, 0, 0)));
		assertEquals(new Vector3i(1, 0, 0), World.getChunkPosAt(new Vector3f(Chunk.SIZE, 0, 0)));
		assertEquals(new Vector3i(0, 0, -1), World.getChunkPosAt(new Vector3f(0, 0, -0.0001f)));
		assertEquals(new Vector3i(0, 0, 0), World.getChunkPosAt(new Vector3f(Chunk.SIZE / 2f, Chunk.SIZE / 2f, Chunk.SIZE / 2f)));
		assertEquals(new Vector3i(1, 1, 1), World.getChunkPosAt(new Vector3f(Chunk.SIZE, Chunk.SIZE, Chunk.SIZE)));
		assertEquals(new Vector3i(4, 4, 4), World.getChunkPosAt(new Vector3f(Chunk.SIZE * 5 - 1, Chunk.SIZE * 5 - 1, Chunk.SIZE * 5 - 1)));
	}

	@Test
	public void testGetLookingAtPos() {
		World world = new World();
		Chunk chunk = new Chunk(0, 0, 0);
		world.addChunk(chunk);
		// Spawn a block in the middle for testing.
		Vector3i blockPos = new Vector3i(7, 7, 7);
		world.setBlockAt(blockPos.x, blockPos.y, blockPos.z, (byte) 1);
		Hit hit;

		// Looking down.
		hit = world.getLookingAtPos(new Vector3f(7.5f, 10, 7.5f), new Vector3f(0, -1, 0), 10);
		assertEquals(blockPos, hit.pos());
		assertEquals(Directions.UP, hit.norm());

		// Looking up.
		hit = world.getLookingAtPos(new Vector3f(7.5f, 5, 7.5f), new Vector3f(0, 1, 0), 10);
		assertEquals(blockPos, hit.pos());
		assertEquals(Directions.DOWN, hit.norm());
	}
}
