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
			for (var v : world.getPalette().toArray()) {
				out.writeFloat(v);
			}
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
			ColorPalette palette = new ColorPalette();
			for (int i = 0; i < ColorPalette.MAX_COLORS; i++) {
				palette.setColor((byte) (i + 1), in.readFloat(), in.readFloat(), in.readFloat());
			}
			world.setPalette(palette);
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
