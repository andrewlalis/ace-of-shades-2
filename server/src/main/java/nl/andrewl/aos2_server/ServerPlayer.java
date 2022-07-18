package nl.andrewl.aos2_server;

import nl.andrewl.aos2_server.logic.PlayerActionManager;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.item.Inventory;
import nl.andrewl.aos_core.model.item.ItemTypes;
import nl.andrewl.aos_core.net.client.PlayerUpdateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * An extension of the base player class with additional information that's
 * needed for the server.
 */
public class ServerPlayer extends Player {
	private static final Logger log = LoggerFactory.getLogger(ServerPlayer.class);

	public static final float HEIGHT = 1.8f;
	public static final float HEIGHT_CROUCH = 1.4f;
	public static final float EYE_HEIGHT = HEIGHT - 0.1f;
	public static final float EYE_HEIGHT_CROUCH = HEIGHT_CROUCH - 0.1f;
	public static final float WIDTH = 0.75f;
	public static final float RADIUS = WIDTH / 2f;

	private final PlayerActionManager actionManager;
	private final Inventory inventory;

	public ServerPlayer(int id, String username) {
		super(id, username);
		this.inventory = new Inventory(new ArrayList<>(), 0);
		this.actionManager = new PlayerActionManager(this);
		inventory.getItemStacks().add(new GunItemStack(ItemTypes.get("Rifle")));
		inventory.getItemStacks().add(new BlockItemStack(ItemTypes.get("Block"), 50, (byte) 1));
	}

	public PlayerActionManager getActionManager() {
		return actionManager;
	}

	public Inventory getInventory() {
		return inventory;
	}

	/**
	 * Helper method to build an update message for this player, to be sent to
	 * various clients.
	 * @return The update message.
	 */
	public PlayerUpdateMessage getUpdateMessage() {
		return new PlayerUpdateMessage(
				id,
				position.x, position.y, position.z,
				velocity.x, velocity.y, velocity.z,
				orientation.x, orientation.y,
				actionManager.getLastInputState().crouching(),
				inventory.getSelectedItemStack().getType().getId()
		);
	}
}
