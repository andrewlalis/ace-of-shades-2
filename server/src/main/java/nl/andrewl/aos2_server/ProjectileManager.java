package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.model.BlockHitTracker;
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
import org.joml.Vector3fc;
import org.joml.Vector3i;

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
	private final Map<Vector3i, BlockHitTracker> blockHitTrackers;

	public ProjectileManager(Server server) {
		this.server = server;
		this.projectiles = new HashMap<>();
		this.removalQueue = new LinkedList<>();
		this.blockHitTrackers = new HashMap<>();
	}

	/**
	 * Spawns any bullets necessary as a result of firing a gun.
	 * @param player The player that fired the gun.
	 * @param gun The gun that is firing the bullet(s).
	 */
	public void spawnBullets(ServerPlayer player, Gun gun) {
		Random rand = ThreadLocalRandom.current();
		Vector3f pos = new Vector3f();
		Vector3f direction = new Vector3f();
		Matrix4f bulletTransform = new Matrix4f();

		for (int i = 0; i < gun.getBulletsPerRound(); i++) {
			int id = nextProjectileId++;
			if (nextProjectileId == Integer.MAX_VALUE) nextProjectileId = 1;

			pos.set(0);
			direction.set(player.getViewVector()).normalize();
			float accuracy = gun.getAccuracy();

			if (player.getActionManager().isScopeEnabled()) {
				bulletTransform.identity()
						.translate(player.getEyePosition())
						.rotate(player.getOrientation().x + (float) Math.PI, Directions.UPf);
				accuracy += (1f - accuracy) / 2f;
			} else {
				bulletTransform.identity()
						.translate(player.getEyePosition())
						.rotate(player.getOrientation().x + (float) Math.PI, Directions.UPf)
						.translate(-0.35f, -0.4f, 0.35f);
			}

			bulletTransform.transformPosition(pos);
			accuracy -= server.getConfig().actions.movementAccuracyDecreaseFactor * player.getVelocity().length();
			float perturbationFactor = (1 - accuracy) / 8;
			direction.x += rand.nextGaussian(0, perturbationFactor);
			direction.y += rand.nextGaussian(0, perturbationFactor);
			direction.z += rand.nextGaussian(0, perturbationFactor);

			Vector3f vel = new Vector3f(direction).normalize()
					.mul(300 * MOVEMENT_FACTOR)
					.add(player.getVelocity());

			ServerProjectile bullet = new ServerProjectile(id, new Vector3f(pos), vel, Projectile.Type.BULLET, player, gun);
			projectiles.put(bullet.getId(), bullet);
			server.getPlayerManager().broadcastUdpMessage(bullet.toMessage(false));
		}
	}

	public void tick(long now, float dt) {
		for (var projectile : projectiles.values()) {
			tickProjectile(projectile, now, dt);
		}
		while (!removalQueue.isEmpty()) {
			ServerProjectile projectile = removalQueue.remove();
			projectiles.remove(projectile.getId());
		}
		// Remove any block hit trackers for blocks whose cooldown period has passed.
		blockHitTrackers.entrySet().removeIf(entry -> now - entry.getValue().getLastHitAt() > server.getConfig().actions.blockBulletDamageCooldown * 1000);
	}

	private void tickProjectile(ServerProjectile projectile, long now, float dt) {
		projectile.getVelocity().y -= server.getConfig().physics.gravity * dt * MOVEMENT_FACTOR;

		// Check for if the bullet will move close enough to a player to hit them.
		Vector3f movement = new Vector3f(projectile.getVelocity()).mul(dt);
		Vector3f direction = new Vector3f(projectile.getVelocity()).normalize();

		// Check first to see if we'll hit a player this tick.
		Vector3f playerHit = null;
		ServerPlayer hitPlayer = null;
		int playerHitType = -1;
		for (ServerPlayer player : server.getPlayerManager().getPlayers()) {
			// Don't allow players to shoot themselves.
			if (projectile.getPlayer() != null && projectile.getPlayer().equals(player)) continue;
			// Don't check for collisions with team players, if friendly fire is disabled.
			if (
					!server.getConfig().actions.friendlyFire &&
					projectile.getPlayer() != null && projectile.getPlayer().getTeam() != null &&
					projectile.getPlayer().getTeam().equals(player.getTeam())
			) continue;

			Vector3f headPos = player.getEyePosition();
			float headRadius = Player.RADIUS;
			Vector3f bodyPos = new Vector3f(player.getPosition().x, player.getPosition().y + 1, player.getPosition().z);
			float bodyRadius = Player.RADIUS;

			Vector3f headIntersect = checkSphereIntersection(projectile.getPosition(), direction, headPos, headRadius);
			if (headIntersect != null) {
				playerHitType = 1;
				playerHit = headIntersect;
				hitPlayer = player;
				break;
			}

			// Check body intersection.
			Vector3f bodyIntersect = checkSphereIntersection(projectile.getPosition(), direction, bodyPos, bodyRadius);
			if (bodyIntersect != null) {
				playerHitType = 2;
				playerHit = bodyIntersect;
				hitPlayer = player;
				break;
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
			handleProjectileBlockHit(hit, projectile, now);
		} else if (playerHit != null && (hit == null || playerHitDist < worldHitDist)) {
			// Bullet struck the player first.
			handleProjectilePlayerHit(playerHitType, hitPlayer, projectile);
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

	/**
	 * Checks for and returns the point at which a ray with origin p and
	 * normalized direction d intersects with a sphere whose center is c, and
	 * whose radius is r.
	 * @param p The ray origin.
	 * @param d The ray's normalized direction.
	 * @param c The sphere's center.
	 * @param r The radius of the sphere.
	 * @return The point of intersection, if one exists.
	 */
	private Vector3f checkSphereIntersection(Vector3fc p, Vector3fc d, Vector3fc c, float r) {
		Vector3f vpc = new Vector3f(c).sub(p);
		if (vpc.dot(d) >= 0) {// Only check for intersections ahead of us.
			Vector3f puv = new Vector3f(d).mul(d.dot(vpc) / d.length());
			Vector3f pc = new Vector3f(p).add(puv);
			Vector3f diff = new Vector3f(c).sub(pc);
			if (diff.length() <= r) {
				Vector3f pcToC = new Vector3f(pc).sub(c);
				float dist = (float) Math.sqrt(r * r - pcToC.lengthSquared());
				float distanceToFirstIntersection;
				if (vpc.length() > r) {
					distanceToFirstIntersection = pcToC.length() - dist;
				} else {
					distanceToFirstIntersection = pcToC.length() + dist;
				}
				return new Vector3f(d).mul(distanceToFirstIntersection).add(p);
			}
		}
		return null;
	}

	private void handleProjectileBlockHit(Hit hit, ServerProjectile projectile, long now) {
		if (!server.getTeamManager().isProtected(hit.pos())) {
			Gun gun = (Gun) projectile.getSourceItem();
			float damage = gun.getBaseDamage();
			BlockHitTracker blockHitTracker = blockHitTrackers.computeIfAbsent(hit.pos(), p -> new BlockHitTracker(now, damage));
			if (blockHitTracker.getDamageAccumulated() >= server.getConfig().actions.blockBulletDamageResistance) {
				server.getWorld().setBlockAt(hit.pos().x, hit.pos().y, hit.pos().z, (byte) 0);
				server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(hit.pos(), server.getWorld()));
				blockHitTrackers.remove(hit.pos());
			} else {
				blockHitTracker.doHit(now, damage);
			}
			int soundVariant = ThreadLocalRandom.current().nextInt(1, 6);
			server.getPlayerManager().broadcastUdpMessage(new SoundMessage("bullet_impact_" + soundVariant, 1, hit.rawPos()));
		}
		deleteProjectile(projectile);
	}

	private void handleProjectilePlayerHit(int playerHitType, ServerPlayer hitPlayer, ServerProjectile projectile) {
		if (!server.getTeamManager().isProtected(hitPlayer)) {
			Gun gun = (Gun) projectile.getSourceItem();
			float damage = gun.getBaseDamage();
			if (playerHitType == 1) {// headshot.
				damage *= 2;
				if (projectile.getPlayer() != null) {
					var shooter = projectile.getPlayer();
					server.getPlayerManager().getHandler(shooter).sendDatagramPacket(new SoundMessage("hit_1", 1, shooter.getPosition(), shooter.getVelocity()));
				}
			} else {
				if (projectile.getPlayer() != null) {
					var shooter = projectile.getPlayer();
					server.getPlayerManager().getHandler(shooter).sendDatagramPacket(new SoundMessage("hit_2", 1, shooter.getPosition(), shooter.getVelocity()));
				}
			}
			hitPlayer.setHealth(hitPlayer.getHealth() - damage);
			Vector3f impactAcceleration = new Vector3f(projectile.getVelocity()).normalize().mul(3);
			hitPlayer.getVelocity().add(impactAcceleration);
			int soundVariant = ThreadLocalRandom.current().nextInt(1, 4);
			server.getPlayerManager().broadcastUdpMessage(new SoundMessage("hurt_" + soundVariant, 1, hitPlayer.getPosition(), hitPlayer.getVelocity()));
			if (hitPlayer.getHealth() == 0) {
				server.getPlayerManager().playerKilled(hitPlayer, projectile.getPlayer());
			} else {
				server.getPlayerManager().getHandler(hitPlayer).sendDatagramPacket(new ClientHealthMessage(hitPlayer.getHealth()));
			}
		}
		deleteProjectile(projectile);
	}

	private void deleteProjectile(ServerProjectile p) {
		removalQueue.add(p);
		server.getPlayerManager().broadcastUdpMessage(p.toMessage(true));
	}
}
