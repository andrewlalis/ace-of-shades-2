package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.item.Inventory;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.record_net.Message;

/**
 * Lightweight packet that's sent when a single item stack in a player's
 * inventory updates.
 */
public record ItemStackMessage(
		int index,
		ItemStack stack
) implements Message {
	public ItemStackMessage(Inventory inv) {
		this(inv.getSelectedIndex(), inv.getSelectedItemStack());
	}
}
