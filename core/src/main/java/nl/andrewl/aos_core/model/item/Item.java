package nl.andrewl.aos_core.model.item;

/**
 * Represents a type of item that a player can have.
 */
public class Item {
	/**
	 * The item's unique id.
	 */
	protected final int id;

	/**
	 * The item's unique name.
	 */
	protected final String name;

	/**
	 * The maximum amount of this item that can be in a stack at once.
	 */
	protected final int maxAmount;

	public Item(int id, String name, int maxAmount) {
		this.id = id;
		this.name = name;
		this.maxAmount = maxAmount;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getMaxAmount() {
		return maxAmount;
	}
}
