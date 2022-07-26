package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;
import org.joml.Vector3f;

/**
 * A message that indicates that a sound has been emitted somewhere in the
 * world, and that clients may need to play the sound.
 */
public record SoundMessage(
		String name,
		float gain,
		float px, float py, float pz,
		float vx, float vy, float vz
) implements Message {
	public SoundMessage(String name, float gain, Vector3f position, Vector3f velocity) {
		this(
				name,
				gain,
				position.x, position.y, position.z,
				velocity.x, velocity.y, velocity.z
		);
	}

	public SoundMessage(String name, float gain, Vector3f position) {
		this(name, gain, position.x, position.y, position.z, 0, 0, 0);
	}

	public Vector3f positionAsVec() {
		return new Vector3f(px, py, pz);
	}

	public Vector3f velocityAsVec() {
		return new Vector3f(vx, vy, vz);
	}
}
