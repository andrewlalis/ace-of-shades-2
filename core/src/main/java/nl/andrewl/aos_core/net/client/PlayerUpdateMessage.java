package nl.andrewl.aos_core.net.client;

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
		float ox, float oy,
		boolean crouching,
		int selectedItemId
) implements Message {

	public void apply(Player p) {
		p.getPosition().set(px, py, pz);
		p.getVelocity().set(vx, vy, vz);
		p.getOrientation().set(ox, oy);
	}
}
