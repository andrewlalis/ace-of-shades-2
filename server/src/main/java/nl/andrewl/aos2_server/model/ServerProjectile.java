package nl.andrewl.aos2_server.model;

import nl.andrewl.aos_core.model.Projectile;
import nl.andrewl.aos_core.net.client.ProjectileMessage;
import org.joml.Vector3f;

/**
 * A bullet with extra information about who shot it, and from where it was
 * shot, so we can properly delete the bullet and know who is responsible
 * for damage it causes.
 */
public class ServerProjectile extends Projectile {
	private final ServerPlayer player;
	private final Vector3f origin;

	public ServerProjectile(int id, Vector3f position, Vector3f velocity, Type type, ServerPlayer player) {
		super(id, position, velocity, type);
		this.player = player;
		this.origin = new Vector3f(position);
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	public Vector3f getOrigin() {
		return origin;
	}

	public float getDistanceTravelled() {
		return origin.distance(position);
	}

	public ProjectileMessage toMessage(boolean destroyed) {
		return new ProjectileMessage(
				id, type,
				position.x, position.y, position.z,
				velocity.x, velocity.y, velocity.z,
				destroyed
		);
	}
}
