package nl.andrewl.aos2_client;

import nl.andrewl.aos2_client.render.chunk.ChunkMesh;
import nl.andrewl.aos2_client.render.chunk.ChunkMeshGenerator;
import nl.andrewl.aos_core.model.world.Chunk;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.net.world.ChunkDataMessage;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A client-side extension of the world model, with information that is only
 * important for the client to know about, like other players and chunk render
 * queues.
 */
public class ClientWorld extends World {
	private final Queue<Chunk> chunkUpdateQueue = new ConcurrentLinkedQueue<>();
	private final Queue<Chunk> chunkRemovalQueue = new ConcurrentLinkedQueue<>();
	private final ChunkMeshGenerator chunkMeshGenerator = new ChunkMeshGenerator();
	private final Map<Chunk, ChunkMesh> chunkMeshes = new ConcurrentHashMap<>();

	@Override
	public void addChunk(Chunk chunk) {
		super.addChunk(chunk);
		chunkUpdateQueue.add(chunk);
	}

	public void addChunk(ChunkDataMessage msg) {
		addChunk(msg.toChunk());
	}

	@Override
	public void removeChunk(Vector3i chunkPos) {
		Chunk chunk = getChunkAt(chunkPos);
		if (chunk != null) {
			chunkRemovalQueue.add(chunk);
			chunkUpdateQueue.remove(chunk);
		}
		super.removeChunk(chunkPos);
	}

	public void updateChunk(ChunkUpdateMessage update) {
		Chunk chunk = getChunkAt(update.getChunkPos());
		if (chunk != null) {
			chunk.setBlockAt(update.lx(), update.ly(), update.lz(), update.newBlock());
			List<Chunk> chunksToReRender = new ArrayList<>(7);
			chunksToReRender.add(chunk);
			// Check if neighboring chunks need to be re-rendered too.
			if (update.lx() == 0) {
				Chunk c = getChunkAt(update.cx() - 1, update.cy(), update.cz());
				if (c != null) chunksToReRender.add(c);
			}
			if (update.ly() == 0) {
				Chunk c = getChunkAt(update.cx(), update.cy() - 1, update.cz());
				if (c != null) chunksToReRender.add(c);
			}
			if (update.lz() == 0) {
				Chunk c = getChunkAt(update.cx(), update.cy(), update.cz() - 1);
				if (c != null) chunksToReRender.add(c);
			}
			if (update.lx() == Chunk.SIZE - 1) {
				Chunk c = getChunkAt(update.cx() + 1, update.cy(), update.cz());
				if (c != null) chunksToReRender.add(c);
			}
			if (update.ly() == Chunk.SIZE - 1) {
				Chunk c = getChunkAt(update.cx(), update.cy() + 1, update.cz());
				if (c != null) chunksToReRender.add(c);
			}
			if (update.lz() == Chunk.SIZE - 1) {
				Chunk c = getChunkAt(update.cx(), update.cy(), update.cz() + 1);
				if (c != null) chunksToReRender.add(c);
			}
			chunkUpdateQueue.addAll(chunksToReRender);
		}
	}

	/**
	 * Call this to process any queued chunk updates, and update chunk meshes.
	 * Only call this method on the main OpenGL context thread!
	 */
	public void processQueuedChunkUpdates() {
		while (!chunkRemovalQueue.isEmpty()) {
			Chunk chunk = chunkRemovalQueue.remove();
			ChunkMesh mesh = chunkMeshes.remove(chunk);
			if (mesh != null) mesh.free();
		}
		while (!chunkUpdateQueue.isEmpty()) {
			Chunk chunk = chunkUpdateQueue.remove();
			ChunkMesh mesh = new ChunkMesh(chunk, this, chunkMeshGenerator);
			ChunkMesh existingMesh = chunkMeshes.get(chunk);
			if (existingMesh != null) existingMesh.free();
			chunkMeshes.put(chunk, mesh);
		}
	}

	public Collection<ChunkMesh> getChunkMeshesToDraw() {
		return chunkMeshes.values();
	}
}
