package nl.andrewl.aos_core.model;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
