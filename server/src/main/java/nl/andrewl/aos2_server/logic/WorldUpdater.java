package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import org.joml.Math;

/**
 * A runnable to run as a separate thread, to periodically update the server's
 * world as players perform actions. This is essentially the "core" of the
 * game engine, as it controls the game's main update pattern.
 */
public class WorldUpdater implements Runnable {
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
		double secondsPerTick = 1.0 / ticksPerSecond;
		final long msPerTick = (long) Math.floor(secondsPerTick * 1_000);
		final long nsPerTick = (long) Math.floor(secondsPerTick * 1_000_000_000);
		System.out.printf("Running world updater at %d ms/tick, or %d ns/tick.%n", msPerTick, nsPerTick);
		running = true;
		while (running) {
			long start = System.nanoTime();
			tick(System.currentTimeMillis());
			long elapsedNs = System.nanoTime() - start;
			if (elapsedNs > nsPerTick) {
				System.err.printf("Took %d ns to do one tick, which is more than the desired %d ns per tick.%n", elapsedNs, nsPerTick);
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

	/**
	 * The main server update method, that runs once on each game tick, and
	 * performs all game state updates.
	 * @param currentTimeMillis The current timestamp for the tick. This may
	 *                          be needed for certain functions in logic.
	 */
	private void tick(long currentTimeMillis) {
		server.getPlayerManager().tick(currentTimeMillis, secondsPerTick);
		server.getProjectileManager().tick(currentTimeMillis, secondsPerTick);
	}
}
