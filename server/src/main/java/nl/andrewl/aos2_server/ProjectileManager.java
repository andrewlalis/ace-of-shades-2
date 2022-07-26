package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos2_server.model.ServerProjectile;
import nl.andrewl.aos_core.Directions;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.Projectile;
import nl.andrewl.aos_core.model.item.Gun;
import nl.andrewl.aos_core.model.world.Hit;
import nl.andrewl.aos_core.net.client.ClientHealthMessage;
import nl.andrewl.aos_core.net.client.SoundMessage;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Component that manages the set of all active projectiles in the world, and
 * performs tick updates for them.
 */
public class ProjectileManager {
	public static final float MOVEMENT_FACTOR = 1f;

	private final Server server;
	private int nextProjectileId = 1;
	private final Map<Integer, ServerProjectile> projectiles;
	private final Queue<ServerProjectile> removalQueue;

	public ProjectileManager(Server server) {
		this.server = server;
		this.projectiles = new HashMap<>();
		this.removalQueue = new LinkedList<>();
	}

	public void spawnBullet(ServerPlayer player, Gun gun) {
		int id = nextProjectileId++;
		if (nextProjectileId == Integer.MAX_VALUE) nextProjectileId = 1;
		Random rand = ThreadLocalRandom.current();

		Vector3f pos = new Vector3f();
		Matrix4f bulletTransform = new Matrix4f()
				.translate(player.getEyePosition())
				.rotate(player.getOrientation().x + (float) Math.PI, Directions.UPf)
				.translate(-0.35f, -0.4f, 0.35f);
		bulletTransform.transformPosition(pos);

		Vector3f direction = new Vector3f(player.getViewVector()).normalize();
		float accuracy = gun.getAccuracy();
		accuracy -= server.getConfig().actions.movementAccuracyDecreaseFactor * player.getVelocity().length();
		float perturbationFactor = (1 - accuracy) / 8;
		direction.x += rand.nextGaussian(0, perturbationFactor);
		direction.y += rand.nextGaussian(0, perturbationFactor);
		direction.z += rand.nextGaussian(0, perturbationFactor);

		Vector3f vel = new Vector3f(direction).normalize()
				.mul(200 * MOVEMENT_FACTOR)
				.add(player.getVelocity());

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
		projectile.getVelocity().y -= server.getConfig().physics.gravity * dt * MOVEMENT_FACTOR;

		// Check for if the bullet will move close enough to a player to hit them.
		Vector3f movement = new Vector3f(projectile.getVelocity()).mul(dt);

		// Check first to see if we'll hit a player this tick.
		Vector3f testPos = new Vector3f();
		Vector3f testMovement = new Vector3f(movement).normalize(0.1f);
		Vector3f playerHit = null;
		ServerPlayer hitPlayer = null;
		int playerHitType = -1;
		for (ServerPlayer player : server.getPlayerManager().getPlayers()) {
			// Don't allow players to shoot themselves.
			if (projectile.getPlayer() != null && projectile.getPlayer().equals(player)) continue;

			Vector3f headPos = player.getEyePosition();
			Vector3f bodyPos = new Vector3f(player.getPosition());
			bodyPos.y += 1.0f;

			// Do a really shitty collision detection... check in 10cm increments if we're close to the player.
			// TODO: Come up with a better collision system.
			testPos.set(projectile.getPosition());
			while (testPos.distanceSquared(projectile.getPosition()) < movement.lengthSquared() && playerHit == null) {
				if (testPos.distanceSquared(headPos) < Player.RADIUS * Player.RADIUS) {
					playerHitType = 1;
					playerHit = new Vector3f(testPos);
					hitPlayer = player;
				} else if (testPos.distanceSquared(bodyPos) < Player.RADIUS * Player.RADIUS) {
					playerHitType = 2;
					playerHit = new Vector3f(testPos);
					hitPlayer = player;
				}
				testPos.add(testMovement);
			}
		}

		// Then check to see if we'll hit the world during this tick.
		Vector3f movementDir = new Vector3f(movement).normalize();
		Hit hit = server.getWorld().getLookingAtPos(projectile.getPosition(), movementDir, movement.length());

		float playerHitDist = Float.MAX_VALUE;
		if (playerHit != null) playerHitDist = projectile.getPosition().distanceSquared(playerHit);
		float worldHitDist = Float.MAX_VALUE;
		if (hit != null) worldHitDist = projectile.getPosition().distanceSquared(hit.rawPos());

		// If we hit the world before the player,
		if (hit != null && (playerHit == null || worldHitDist < playerHitDist)) {
			// Bullet struck the world first.
			server.getWorld().setBlockAt(hit.pos().x, hit.pos().y, hit.pos().z, (byte) 0);
			server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(hit.pos(), server.getWorld()));
			int soundVariant = ThreadLocalRandom.current().nextInt(1, 6);
			server.getPlayerManager().broadcastUdpMessage(new SoundMessage("bullet_impact_" + soundVariant, 1, hit.rawPos()));
			deleteProjectile(projectile);
		} else if (playerHit != null && (hit == null || playerHitDist < worldHitDist)) {
			// Bullet struck the player first.
			float damage = 0.4f;
			if (playerHitType == 1) damage *= 2;
			hitPlayer.setHealth(hitPlayer.getHealth() - damage);
			int soundVariant = ThreadLocalRandom.current().nextInt(1, 4);
			server.getPlayerManager().broadcastUdpMessage(new SoundMessage("hurt_" + soundVariant, 1, hitPlayer.getPosition(), hitPlayer.getVelocity()));
			if (hitPlayer.getHealth() == 0) {
				System.out.println("Player killed!!!");
				server.getPlayerManager().playerKilled(hitPlayer);
			} else {
				server.getPlayerManager().getHandler(hitPlayer).sendDatagramPacket(new ClientHealthMessage(hitPlayer.getHealth()));
			}
			deleteProjectile(projectile);
		} else {
			// Bullet struck nothing.
			projectile.getPosition().add(movement);
			if (projectile.getDistanceTravelled() > 500) {
				deleteProjectile(projectile);
			} else {
				server.getPlayerManager().broadcastUdpMessage(projectile.toMessage(false));
			}
		}
	}

	private void deleteProjectile(ServerProjectile p) {
		removalQueue.add(p);
		server.getPlayerManager().broadcastUdpMessage(p.toMessage(true));
	}
}
