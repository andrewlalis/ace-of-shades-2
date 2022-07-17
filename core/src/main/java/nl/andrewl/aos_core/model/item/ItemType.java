package nl.andrewl.aos_core.model.item;

/**
 * Represents a type of item that a player can have.
 */
public class ItemType {
	private final int id;
	private final String name;
	private final int maxAmount;

	public ItemType(int id, String name, int maxAmount) {
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
