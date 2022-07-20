package nl.andrewl.aos_core.model.world;

import org.joml.Vector3f;

import java.io.*;
import java.util.Map;

/**
 * Utility class for reading and writing worlds to files.
 */
public final class WorldIO {
	/**
	 * Writes a world to an output stream.
	 * @param world The world to write.
	 * @param out The output stream to write to.
	 * @throws IOException If an exception occurs.
	 */
	public static void write(World world, OutputStream out) throws IOException {
		var d = new DataOutputStream(out);
		// Write color palette.
		for (var v : world.getPalette().toArray()) {
			d.writeFloat(v);
		}
		// Write spawn points.
		var spawnPoints = world.getSpawnPoints();
		d.writeInt(spawnPoints.size());
		var sortedEntries = spawnPoints.entrySet().stream()
				.sorted(Map.Entry.comparingByKey()).toList();
		for (var entry : sortedEntries) {
			d.writeUTF(entry.getKey());
			d.writeFloat(entry.getValue().x());
			d.writeFloat(entry.getValue().y());
			d.writeFloat(entry.getValue().z());
		}
		// Write chunks.
		var chunks = world.getChunkMap().values();
		d.writeInt(chunks.size());
		for (var chunk : chunks) {
			d.writeInt(chunk.getPosition().x);
			d.writeInt(chunk.getPosition().y);
			d.writeInt(chunk.getPosition().z);
			d.write(chunk.getBlocks());
		}
	}

	/**
	 * Reads a world from an input stream.
	 * @param in The input stream to read from.
	 * @return The world which was read.
	 * @throws IOException If an exception occurs.
	 */
	public static World read(InputStream in) throws IOException {
		World world = new World();
		var d = new DataInputStream(in);
		// Read color palette.
		ColorPalette palette = new ColorPalette();
		for (int i = 0; i < ColorPalette.MAX_COLORS; i++) {
			palette.setColor((byte) (i + 1), d.readFloat(), d.readFloat(), d.readFloat());
		}
		world.setPalette(palette);
		// Read spawn points.
		int spawnPointCount = d.readInt();
		for (int i = 0; i < spawnPointCount; i++) {
			String name = d.readUTF();
			Vector3f location = new Vector3f(d.readFloat(), d.readFloat(), d.readFloat());
			world.setSpawnPoint(name, location);
		}
		// Read chunks.
		int chunkCount = d.readInt();
		for (int i = 0; i < chunkCount; i++) {
			Chunk chunk = new Chunk(
					d.readInt(),
					d.readInt(),
					d.readInt(),
					d.readNBytes(Chunk.TOTAL_SIZE)
			);
			world.addChunk(chunk);
		}
		return world;
	}
}
