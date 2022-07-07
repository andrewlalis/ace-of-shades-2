package nl.andrewl.aos_core.model;

import nl.andrewl.aos_core.MathUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.joml.Math.*;

/**
 * Basic information about a player that both the client and server should
 * know.
 */
public class Player {
	/**
	 * The player's position. This is the position of their feet. So if a
	 * player is standing on a block at y=5 (block occupies space from 4 to 5)
	 * then the player's y coordinate is y=6.0. The x and z coordinates are
	 * simply the center of the player.
	 */
	private final Vector3f position;

	/**
	 * The player's velocity in each of the coordinate axes.
	 */
	private final Vector3f velocity;

	/**
	 * The player's orientation. The x component refers to rotation about the
	 * vertical axis, and the y component refers to rotation about the
	 * horizontal axis. The x component is limited to between 0 and 2 PI, where
	 * x=0 means the player is looking towards the +Z axis. x increases in a
	 * counterclockwise fashion.
	 * The y component is limited to between 0 and PI, with y=0 looking
	 * straight down, and y=PI looking straight up.
	 */
	private final Vector2f orientation;

	/**
	 * A vector that's internally re-computed each time the player's
	 * orientation changes, and represents unit vector pointing in the
	 * direction the player is looking.
	 */
	private final Vector3f viewVector;

	private final String username;
	private final int id;

	public Player(int id, String username) {
		this.position = new Vector3f();
		this.velocity = new Vector3f();
		this.orientation = new Vector2f();
		this.viewVector = new Vector3f();
		this.id = id;
		this.username = username;
	}

	public Vector3f getPosition() {
		return position;
	}

	public void setPosition(Vector3f position) {
		this.position.set(position);
	}

	public Vector3f getVelocity() {
		return velocity;
	}

	public void setVelocity(Vector3f velocity) {
		this.velocity.set(velocity);
	}

	public Vector2f getOrientation() {
		return orientation;
	}

	public void setOrientation(float x, float y) {
		orientation.set(MathUtils.normalize(x, 0, PI * 2), MathUtils.clamp(y, 0, (float) PI));
		y = orientation.y + (float) PI / 2f;
		viewVector.set(sin(orientation.x) * cos(y), -sin(y), cos(orientation.x) * cos(y)).normalize();
	}

	public String getUsername() {
		return username;
	}

	public int getId() {
		return id;
	}

	public Vector3f getViewVector() {
		return viewVector;
	}
}
