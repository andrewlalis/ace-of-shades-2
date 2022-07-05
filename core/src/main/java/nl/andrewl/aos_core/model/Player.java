package nl.andrewl.aos_core.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Player {
	private final Vector3f position;
	private final Vector3f velocity;
	private final Vector2f orientation;
	private final String username;

	public Player(String username) {
		this.position = new Vector3f();
		this.velocity = new Vector3f();
		this.orientation = new Vector2f();
		this.username = username;
	}
}
