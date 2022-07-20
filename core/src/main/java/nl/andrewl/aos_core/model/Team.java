package nl.andrewl.aos_core.model;

import org.joml.Vector3f;

/**
 * A team is a group of players in a world that should work together to
 * achieve some goal. Teams belong to a server directly, and persist even if
 * the world is changed.
 */
public class Team {
	/**
	 * The internal id that this team is assigned.
	 */
	private final int id;

	/**
	 * The name of the team.
	 */
	private final String name;

	/**
	 * The team's color, used to identify players and structures belonging to
	 * this team.
	 */
	private final Vector3f color;

	/**
	 * The team's spawn point, in the current world.
	 */
	private final Vector3f spawnPoint;

	public Team(int id, String name, Vector3f color, Vector3f spawnPoint) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.spawnPoint = spawnPoint;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Vector3f getColor() {
		return color;
	}

	public Vector3f getSpawnPoint() {
		return spawnPoint;
	}

	public void setSpawnPoint(Vector3f p) {
		spawnPoint.set(p);
	}
}
