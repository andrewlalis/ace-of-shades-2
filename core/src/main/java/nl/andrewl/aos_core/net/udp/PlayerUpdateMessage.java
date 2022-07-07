package nl.andrewl.aos_core.net.udp;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.record_net.Message;

/**
 * This message is sent by the server to clients whenever a player has updated
 * in some way, like movement or orientation or held items.
 */
public record PlayerUpdateMessage(
		int clientId,
		float px, float py, float pz,
		float vx, float vy, float vz,
		float ox, float oy
) implements Message {
	public PlayerUpdateMessage(Player player) {
		this(
				player.getId(),
				player.getPosition().x, player.getPosition().y, player.getPosition().z,
				player.getVelocity().x, player.getVelocity().y, player.getVelocity().z,
				player.getOrientation().x, player.getOrientation().y
		);
	}
}
