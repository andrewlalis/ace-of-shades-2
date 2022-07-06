package nl.andrewl.aos_core.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Player {
	private final Vector3f position;
	private final Vector3f velocity;
	private final Vector2f orientation;
	private final String username;
	private final int id;

	public Player(int id, String username) {
		this.position = new Vector3f();
		this.velocity = new Vector3f();
		this.orientation = new Vector2f();
		this.id = id;
		this.username = username;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Vector3f getVelocity() {
		return velocity;
	}

	public Vector2f getOrientation() {
		return orientation;
	}

	public String getUsername() {
		return username;
	}

	public int getId() {
		return id;
	}
}
