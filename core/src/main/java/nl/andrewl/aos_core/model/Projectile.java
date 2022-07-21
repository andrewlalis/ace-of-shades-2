package nl.andrewl.aos_core.model;

import org.joml.Vector3f;

/**
 * Represents a flying projectile object in the world (usually a bullet).
 * All projectiles have their orientation dependent on their velocity vector.
 */
public class Projectile {
	protected final int id;
	protected final Vector3f position;
	protected final Vector3f velocity;

	public enum Type {BULLET}
	protected final Type type;

	public Projectile(int id, Vector3f position, Vector3f velocity, Type type) {
		this.id = id;
		this.position = position;
		this.velocity = velocity;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Vector3f getVelocity() {
		return velocity;
	}
}
