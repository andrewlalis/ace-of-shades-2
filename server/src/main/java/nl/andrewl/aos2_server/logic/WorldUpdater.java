package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import org.joml.Math;
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
	private final float secondsPerTick;
	private volatile boolean running;

	public WorldUpdater(Server server, float ticksPerSecond) {
		this.server = server;
		this.ticksPerSecond = ticksPerSecond;
		this.secondsPerTick = 1.0f / ticksPerSecond;
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
			player.getActionManager().tick(secondsPerTick, server.getWorld(), server);
			if (player.getActionManager().isUpdated()) server.getPlayerManager().broadcastUdpMessage(player.getUpdateMessage());
		}
	}
}
