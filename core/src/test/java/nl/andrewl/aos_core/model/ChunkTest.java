package nl.andrewl.aos_core.model;

import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkTest {
	@Test
	public void testCoordinateIndexConversion() {
		assertEquals(0, Chunk.xyzToIdx(0, 0, 0));
		assertEquals(-1, Chunk.xyzToIdx(-1, 0, 0));
		assertEquals(-1, Chunk.xyzToIdx(Chunk.SIZE, 0, 0));
		assertEquals(1, Chunk.xyzToIdx(0, 0, 1));
		assertEquals(Chunk.SIZE, Chunk.xyzToIdx(0, 1, 0));
		assertEquals(Chunk.SIZE * Chunk.SIZE, Chunk.xyzToIdx(1, 0, 0));
		assertEquals(Chunk.SIZE * Chunk.SIZE + 1, Chunk.xyzToIdx(1, 0, 1));
		assertEquals(Chunk.TOTAL_SIZE - 1, Chunk.xyzToIdx(Chunk.SIZE - 1, Chunk.SIZE - 1, Chunk.SIZE - 1));
		for (int x = 0; x < Chunk.SIZE; x++) {
			for (int y = 0; y < Chunk.SIZE; y++) {
				for (int z = 0; z < Chunk.SIZE; z++) {
					int idx = x * Chunk.SIZE * Chunk.SIZE + y * Chunk.SIZE + z;
					assertEquals(idx, Chunk.xyzToIdx(x, y, z));
				}
			}
		}

		assertEquals(new Vector3i(0, 0, 0), Chunk.idxToXyz(0));
		assertEquals(new Vector3i(0, 0, 2), Chunk.idxToXyz(2));
		assertEquals(new Vector3i(1, 1, 1), Chunk.idxToXyz(Chunk.SIZE * Chunk.SIZE + Chunk.SIZE + 1));
		assertEquals(new Vector3i(Chunk.SIZE - 1, Chunk.SIZE - 1, Chunk.SIZE - 1), Chunk.idxToXyz(Chunk.TOTAL_SIZE - 1));
		assertEquals(new Vector3i(-1, -1, -1), Chunk.idxToXyz(Chunk.TOTAL_SIZE));

		for (int x = 0; x < Chunk.SIZE; x++) {
			for (int y = 0; y < Chunk.SIZE; y++) {
				for (int z = 0; z < Chunk.SIZE; z++) {
					int idx = x * Chunk.SIZE * Chunk.SIZE + y * Chunk.SIZE + z;
					assertEquals(new Vector3i(x, y, z), Chunk.idxToXyz(idx));
				}
			}
		}
	}

	@Test
	public void testGetBlockAt() {
		Chunk chunk = Chunk.random(new Vector3i(0, 0, 0), new Random(1));
		for (int i = 0; i < Chunk.TOTAL_SIZE; i++) {
			assertTrue(chunk.getBlockAt(Chunk.idxToXyz(i)) > 0);
		}
		assertEquals(0, chunk.getBlockAt(-1, 0, 0));
		assertEquals(0, chunk.getBlockAt(16, 0, 5));
	}
}
