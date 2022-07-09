package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

/**
 * An announcement message that's broadcast to all players when a new player
 * joins, so that they can add that player to their world.
 */
public record PlayerJoinMessage(
		int id, String username,
		float px, float py, float pz,
		float vx, float vy, float vz,
		float ox, float oy
) implements Message {}
