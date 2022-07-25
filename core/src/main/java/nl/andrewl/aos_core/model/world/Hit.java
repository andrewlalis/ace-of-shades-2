package nl.andrewl.aos_core.model.world;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

/**
 * Represents the point at which a ray hits a block, often used when casting a
 * ray from a player's location to see if they break or place a block.
 */
public record Hit (
		Vector3i pos,
		Vector3ic norm,
		Vector3f rawPos
) {}
