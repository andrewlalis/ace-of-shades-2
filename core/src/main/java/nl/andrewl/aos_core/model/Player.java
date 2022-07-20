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
	public static final float HEIGHT = 1.8f;
	public static final float HEIGHT_CROUCH = 1.4f;
	public static final float EYE_HEIGHT = HEIGHT - 0.1f;
	public static final float EYE_HEIGHT_CROUCH = HEIGHT_CROUCH - 0.1f;
	public static final float WIDTH = 0.75f;
	public static final float RADIUS = WIDTH / 2f;

	/**
	 * The player's position. This is the position of their feet. So if a
	 * player is standing on a block at y=5 (block occupies space from 4 to 5)
	 * then the player's y coordinate is y=6.0. The x and z coordinates are
	 * simply the center of the player.
	 */
	protected final Vector3f position;

	/**
	 * The player's velocity in each of the coordinate axes.
	 */
	protected final Vector3f velocity;

	/**
	 * The player's orientation. The x component refers to rotation about the
	 * vertical axis, and the y component refers to rotation about the
	 * horizontal axis. The x component is limited to between 0 and 2 PI, where
	 * x=0 means the player is looking towards the +Z axis. x increases in a
	 * counterclockwise fashion.
	 * The y component is limited to between 0 and PI, with y=0 looking
	 * straight down, and y=PI looking straight up.
	 */
	protected final Vector2f orientation;

	/**
	 * Whether this player is crouching or not. This affects the player's
	 * height, eye-level, speed, and accuracy.
	 */
	protected boolean crouching = false;

	/**
	 * A vector that's internally re-computed each time the player's
	 * orientation changes, and represents unit vector pointing in the
	 * direction the player is looking.
	 */
	protected final Vector3f viewVector;

	/**
	 * The player's name.
	 */
	protected final String username;

	/**
	 * The player's unique id that it was assigned by the server.
	 */
	protected final int id;

	/**
	 * The team that this player belongs to. This might be null.
	 */
	protected Team team;

	public Player(int id, String username, Team team) {
		this.position = new Vector3f();
		this.velocity = new Vector3f();
		this.orientation = new Vector2f();
		this.viewVector = new Vector3f();
		this.id = id;
		this.username = username;
		this.team = team;
	}

	public Player(int id, String username) {
		this(id, username, null);
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

	public boolean isCrouching() {
		return crouching;
	}

	public void setCrouching(boolean crouching) {
		this.crouching = crouching;
	}

	public String getUsername() {
		return username;
	}

	public int getId() {
		return id;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public Vector3f getViewVector() {
		return viewVector;
	}

	public Vector3f getRightVector() {
		float x = orientation.x - (float) (Math.PI / 2);
		return new Vector3f(
				sin(x),
				0,
				cos(x)
		).normalize();
	}

	public float getEyeHeight() {
		return crouching ? EYE_HEIGHT_CROUCH : EYE_HEIGHT;
	}

	public Vector3f getEyePosition() {
		return new Vector3f(
				position.x,
				position.y + getEyeHeight(),
				position.z
		);
	}

	public float getCurrentHeight() {
		return crouching ? HEIGHT_CROUCH : HEIGHT;
	}
}
