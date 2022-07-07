package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.net.udp.PlayerUpdateMessage;
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
		var p = player.getPosition();

		// Apply deceleration to  the player before computing any input-derived acceleration.
		if (v.length() > 0) {
			Vector3f deceleration = new Vector3f(v).negate().normalize().mul(0.1f);
			v.add(deceleration);
			if (v.length() < 0.1f) {
				v.set(0);
			}
			updated = true;
		}

		Vector3f a = new Vector3f();
		var inputState = player.getLastInputState();
		if (inputState.forward()) a.z -= 1;
		if (inputState.backward()) a.z += 1;
		if (inputState.left()) a.x -= 1;
		if (inputState.right()) a.x += 1;
		if (inputState.jumping()) a.y += 1; // TODO: check if on ground.
		if (inputState.crouching()) a.y -= 1; // TODO: do crouching instead of down.
		if (a.lengthSquared() > 0) {
			a.normalize();
			Matrix4f moveTransform = new Matrix4f();
			moveTransform.rotate(player.getOrientation().x, new Vector3f(0, 1, 0));
			moveTransform.transformDirection(a);
			v.add(a);
			final float maxSpeed = 0.25f; // Blocks per tick.
			if (v.length() > maxSpeed) v.normalize(maxSpeed);
			updated = true;
		}

		if (v.lengthSquared() > 0) {
			p.add(v);
			updated = true;
		}

		if (updated) {
			server.getPlayerManager().broadcastUdpMessage(new PlayerUpdateMessage(player));
		}
	}
}
