package nl.andrewl.aos2_client.render;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class PlayerMeshGenerator {
	private final FloatBuffer vertexBuffer;
	private final IntBuffer indexBuffer;

	private final Vector3i pos = new Vector3i();
	private final Vector3f color = new Vector3f();
	private final Vector3f norm = new Vector3f();

	public PlayerMeshGenerator() {
		vertexBuffer = BufferUtils.createFloatBuffer(1000);
		indexBuffer = BufferUtils.createIntBuffer(100);
	}

//	public PlayerMesh generateMesh() {
//
//	}
}
