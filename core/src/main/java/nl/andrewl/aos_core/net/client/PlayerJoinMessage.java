package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.ItemTypes;
import nl.andrewl.record_net.Message;

/**
 * An announcement message that's broadcast to all players when a new player
 * joins, so that they can add that player to their world.
 */
public record PlayerJoinMessage(
		int id, String username, int teamId,
		float px, float py, float pz,
		float vx, float vy, float vz,
		float ox, float oy,
		boolean crouching,
		int selectedItemId
) implements Message {
	public PlayerJoinMessage(Player player) {
		this(
				player.getId(), player.getUsername(), player.getTeam() == null ? -1 : player.getTeam().getId(),
				player.getPosition().x, player.getPosition().y, player.getPosition().z,
				player.getVelocity().x, player.getVelocity().y, player.getVelocity().z,
				player.getOrientation().x, player.getOrientation().y,
				player.isCrouching(),
				ItemTypes.BLOCK.getId()
		);
	}

	public Player toPlayer() {
		Player p = new Player(id, username);
		p.getPosition().set(px, py, pz);
		p.getVelocity().set(vx, vy, vz);
		p.getOrientation().set(ox, oy);
		return p;
	}
}
