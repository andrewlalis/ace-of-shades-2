package nl.andrewl.aos_core.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for reading and writing worlds to files.
 */
public final class WorldIO {
	public static void write(World world, Path path) throws IOException {
		var chunks = world.getChunkMap().values();
		try (var os = Files.newOutputStream(path)) {
			var out = new DataOutputStream(os);
			out.writeInt(chunks.size());
			for (var chunk : chunks) {
				out.writeInt(chunk.getPosition().x);
				out.writeInt(chunk.getPosition().y);
				out.writeInt(chunk.getPosition().z);
				out.write(chunk.getBlocks());
			}
		}
	}

	public static World read(Path path) throws IOException {
		World world = new World();
		try (var is = Files.newInputStream(path)) {
			var in = new DataInputStream(is);
			int chunkCount = in.readInt();
			for (int i = 0; i < chunkCount; i++) {
				Chunk chunk = new Chunk(
						in.readInt(),
						in.readInt(),
						in.readInt(),
						in.readNBytes(Chunk.TOTAL_SIZE)
				);
				world.addChunk(chunk);
			}
		}
		return world;
	}
}
