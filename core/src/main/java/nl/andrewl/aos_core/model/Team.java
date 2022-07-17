package nl.andrewl.aos_core.model;

import org.joml.Vector3f;

/**
 * A team is a group of players in a world that should work together to
 * achieve some goal.
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

	public Team(int id, String name, Vector3f color) {
		this.id = id;
		this.name = name;
		this.color = color;
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
}
