package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos2_server.model.ServerProjectile;
import nl.andrewl.aos_core.model.Projectile;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ProjectileManager {
	private final Server server;
	private int nextProjectileId = 1;
	private final Map<Integer, ServerProjectile> projectiles;
	private final Queue<ServerProjectile> removalQueue;

	public ProjectileManager(Server server) {
		this.server = server;
		this.projectiles = new HashMap<>();
		this.removalQueue = new LinkedList<>();
	}

	public void spawnBullet(ServerPlayer player) {
		int id = nextProjectileId++;
		if (nextProjectileId == Integer.MAX_VALUE) nextProjectileId = 1;
		Vector3f pos = new Vector3f(player.getEyePosition());
		Vector3f vel = new Vector3f(player.getViewVector()).normalize().mul(300);
		ServerProjectile bullet = new ServerProjectile(id, pos, vel, Projectile.Type.BULLET, player);
		projectiles.put(bullet.getId(), bullet);
		server.getPlayerManager().broadcastUdpMessage(bullet.toMessage(false));
	}

	public void tick(float dt) {
		for (var projectile : projectiles.values()) {
			tickProjectile(projectile, dt);
		}
		while (!removalQueue.isEmpty()) {
			ServerProjectile projectile = removalQueue.remove();
			projectiles.remove(projectile.getId());
		}
	}

	private void tickProjectile(ServerProjectile projectile, float dt) {
		projectile.getVelocity().y -= server.getConfig().physics.gravity * dt;
		// TODO: Check if bullet will hit anything, like blocks or players, if it follows current velocity.
		Vector3f movement = new Vector3f(projectile.getVelocity()).mul(dt);

		projectile.getPosition().add(movement);
		if (projectile.getDistanceTravelled() > 500) {
			removalQueue.add(projectile);
			server.getPlayerManager().broadcastUdpMessage(projectile.toMessage(true));
		} else {
			server.getPlayerManager().broadcastUdpMessage(projectile.toMessage(false));
		}
	}
}
