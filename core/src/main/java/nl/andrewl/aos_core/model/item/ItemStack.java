package nl.andrewl.aos_core.model.item;

/**
 * Represents a stack of items in the player's inventory. This is generally
 * a type of item, and the amount of it.
 */
public class ItemStack {
	private final Item type;
	private int amount;

	public ItemStack(Item type, int amount) {
		this.type = type;
		this.amount = amount;
	}

	public Item getType() {
		return type;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
}
