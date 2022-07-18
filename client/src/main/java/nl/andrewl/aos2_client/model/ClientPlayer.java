package nl.andrewl.aos2_client.model;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.Inventory;

import java.util.ArrayList;

public class ClientPlayer extends Player {
	private final Inventory inventory;

	public ClientPlayer(int id, String username) {
		super(id, username);
		this.inventory = new Inventory(new ArrayList<>(), 0);
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inv) {
		this.inventory.getItemStacks().clear();
		this.inventory.getItemStacks().addAll(inv.getItemStacks());
		this.inventory.setSelectedIndex(inv.getSelectedIndex());
	}
}
