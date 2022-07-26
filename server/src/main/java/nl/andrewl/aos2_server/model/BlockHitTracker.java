package nl.andrewl.aos2_server.model;

/**
 * A simple component that's used to track an individual block's accumulated
 * damage from consecutive bullet strikes. This is used to allow for blocks to
 * take multiple hits before being destroyed.
 */
public class BlockHitTracker {
	private long lastHitAt;
	private float damageAccumulated;

	public BlockHitTracker(long now, float initialDamage) {
		this.lastHitAt = now;
		damageAccumulated = initialDamage;
	}

	public void doHit(long now, float damage) {
		lastHitAt = now;
		damageAccumulated += damage;
	}

	public long getLastHitAt() {
		return lastHitAt;
	}

	public float getDamageAccumulated() {
		return damageAccumulated;
	}
}
