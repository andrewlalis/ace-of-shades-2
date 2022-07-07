package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runnable to run as a separate thread, to periodically update the server's
 * world as players perform actions. This is essentially the "core" of the
 * game engine, as it controls the game's main update pattern.
 */
public class WorldUpdater implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(WorldUpdater.class);

	private final Server server;
	private final float ticksPerSecond;
	private volatile boolean running;

	public WorldUpdater(Server server, float ticksPerSecond) {
		this.server = server;
		this.ticksPerSecond = ticksPerSecond;
	}

	public void shutdown() {
		running = false;
	}

	@Override
	public void run() {
		final long nsPerTick = (long) Math.floor((1.0 / ticksPerSecond) * 1_000_000_000.0);
		log.debug("Running world updater at {} ticks per second, or {} ns per tick.", ticksPerSecond, nsPerTick);
		running = true;
		while (running) {
			long start = System.nanoTime();
			tick();
			long elapsedNs = System.nanoTime() - start;
			if (elapsedNs > nsPerTick) {
				log.warn("Took {} ns to do one tick, which is more than the desired {} ns per tick.", elapsedNs, nsPerTick);
			} else {
				long sleepTime = nsPerTick - elapsedNs;
				long ms = sleepTime / 1_000_000;
				int nanos = (int) (sleepTime % 1_000_000);
				try {
					Thread.sleep(ms, nanos);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void tick() {
		for (var player : server.getPlayerManager().getPlayers()) {
			updatePlayerMovement(player);
		}
	}

	private void updatePlayerMovement(ServerPlayer player) {
		boolean updated = false;
		var v = player.getVelocity();
		var hv = new Vector3f(v.x, 0, v.z);
		var p = player.getPosition();

		// Check if we have a negative velocity that will cause us to fall through a block next tick.
		float nextTickY = p.y + v.y;
		if (server.getWorld().getBlockAt(new Vector3f(p.x, nextTickY, p.z)) != 0) {
			// Find the first block we'll hit and set the player down on that.
			int floorY = (int) Math.floor(p.y) - 1;
			while (true) {
				if (server.getWorld().getBlockAt(new Vector3f(p.x, floorY, p.z)) != 0) {
					p.y = floorY + 1f;
					v.y = 0;
					break;
				} else {
					floorY--;
				}
			}
		}

		// Check if the player is on the ground.
		boolean grounded = (Math.floor(p.y) == p.y && server.getWorld().getBlockAt(new Vector3f(p.x, p.y - 0.0001f, p.z)) != 0);

		if (!grounded) {
			v.y -= 0.1f;
		}

		// Apply horizontal deceleration to the player before computing any input-derived acceleration.
		if (grounded && hv.length() > 0) {
			Vector3f deceleration = new Vector3f(hv).negate().normalize().mul(0.1f);
			hv.add(deceleration);
			if (hv.length() < 0.1f) {
				hv.set(0);
			}
			v.x = hv.x;
			v.z = hv.z;
			updated = true;
		}

		Vector3f a = new Vector3f();
		var inputState = player.getLastInputState();
		if (inputState.jumping() && grounded) {
			v.y = 0.6f;
		}

		// Compute horizontal motion separately.
		if (grounded) {
			if (inputState.forward()) a.z -= 1;
			if (inputState.backward()) a.z += 1;
			if (inputState.left()) a.x -= 1;
			if (inputState.right()) a.x += 1;
//			if (inputState.crouching()) a.y -= 1; // TODO: do crouching instead of down.
			if (a.lengthSquared() > 0) {
				a.normalize();
				Matrix4f moveTransform = new Matrix4f();
				moveTransform.rotate(player.getOrientation().x, new Vector3f(0, 1, 0));
				moveTransform.transformDirection(a);
				hv.add(a);

				final float maxSpeed = 0.25f; // Blocks per tick.
				if (hv.length() > maxSpeed) {
					hv.normalize(maxSpeed);
				}
				v.x = hv.x;
				v.z = hv.z;
				updated = true;
			}
		}

		// Apply velocity to the player's position.
		if (v.lengthSquared() > 0) {
			p.add(v);
			updated = true;
		}

		if (updated) {
			server.getPlayerManager().broadcastUdpMessage(new PlayerUpdateMessage(player));
		}
	}
}
