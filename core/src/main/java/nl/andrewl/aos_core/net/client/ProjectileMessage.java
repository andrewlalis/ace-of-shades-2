package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.Projectile;
import nl.andrewl.record_net.Message;

/**
 * A simple message with an update about a bullet. Instead of having a separate
 * message to indicate that a bullet has been destroyed, we add it to the
 * normal bullet message.
 */
public record ProjectileMessage(
		int id, Projectile.Type type,
		float px, float py, float pz,
		float vx, float vy, float vz,
		boolean destroyed
) implements Message {}
