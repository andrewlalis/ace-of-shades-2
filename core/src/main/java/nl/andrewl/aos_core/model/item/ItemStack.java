package nl.andrewl.aos_core.model.item;

/**
 * Represents a stack of items in the player's inventory. This is generally
 * a type of item, and the amount of it.
 */
public class ItemStack {
	private final ItemType type;
	private int amount;

	public ItemStack(ItemType type, int amount) {
		this.type = type;
		this.amount = amount;
	}

	public Object getType() {
		return type;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
}
