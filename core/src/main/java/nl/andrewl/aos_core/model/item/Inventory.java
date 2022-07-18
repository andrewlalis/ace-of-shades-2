package nl.andrewl.aos_core.model.item;

import java.util.List;

/**
 * Represents the contents and current state of a player's inventory.
 */
public class Inventory {
	/**
	 * The list of item stacks in the inventory.
	 */
	private final List<ItemStack> itemStacks;

	/**
	 * The index of the selected item stack.
	 */
	private int selectedIndex;

	public Inventory(List<ItemStack> itemStacks, int selectedIndex) {
		this.itemStacks = itemStacks;
		this.selectedIndex = selectedIndex;
	}

	public List<ItemStack> getItemStacks() {
		return itemStacks;
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public ItemStack getSelectedItemStack() {
		return itemStacks.get(selectedIndex);
	}

	public void setSelectedIndex(int newIndex) {
		if (newIndex < 0) newIndex = 0;
		if (newIndex > itemStacks.size() - 1) newIndex = itemStacks.size() - 1;
		this.selectedIndex = newIndex;
	}
}
