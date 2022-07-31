package nl.andrewl.aos2_server.model;

import nl.andrewl.aos2_server.logic.PlayerActionManager;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.item.Inventory;
import nl.andrewl.aos_core.model.item.ItemTypes;
import nl.andrewl.aos_core.net.client.PlayerUpdateMessage;

import java.util.ArrayList;

/**
 * An extension of the base player class with additional information that's
 * needed for the server.
 */
public class ServerPlayer extends Player {
	private final PlayerActionManager actionManager;
	private final Inventory inventory;

	/**
	 * The player's health, from 0 to 1, where <= 0 means death, and 1 means
	 * full health.
	 */
	private float health;

	private int deathCount;
	private int killCount;

	public ServerPlayer(int id, String username) {
		super(id, username);
		this.inventory = new Inventory(new ArrayList<>(), 0);
		this.health = 1f;
		this.actionManager = new PlayerActionManager(this);
		inventory.getItemStacks().add(new GunItemStack(ItemTypes.RIFLE));
		inventory.getItemStacks().add(new GunItemStack(ItemTypes.AK_47));
		inventory.getItemStacks().add(new GunItemStack(ItemTypes.WINCHESTER));
		inventory.getItemStacks().add(new BlockItemStack(ItemTypes.BLOCK, 50, (byte) 1));
	}

	public PlayerActionManager getActionManager() {
		return actionManager;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public float getHealth() {
		return health;
	}

	public void setHealth(float health) {
		if (health > 1) health = 1;
		if (health < 0) health = 0;
		this.health = health;
	}

	public int getDeathCount() {
		return deathCount;
	}

	public void incrementDeathCount() {
		deathCount++;
	}

	public int getKillCount() {
		return killCount;
	}

	public void incrementKillCount() {
		killCount++;
	}

	/**
	 * Helper method to build an update message for this player, to be sent to
	 * various clients.
	 * @return The update message.
	 */
	public PlayerUpdateMessage getUpdateMessage(long timestamp) {
		return new PlayerUpdateMessage(
				id, timestamp,
				position.x, position.y, position.z,
				velocity.x, velocity.y, velocity.z,
				orientation.x, orientation.y,
				actionManager.getInput().crouching(),
				inventory.getSelectedItemStack().getType().getId(),
				mode
		);
	}
}
