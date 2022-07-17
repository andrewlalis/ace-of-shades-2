package nl.andrewl.aos_core.model.item;

public class BlockItemStack extends ItemStack {
	private int selectedValue = 1;

	public BlockItemStack(BlockItem item, int amount) {
		super(item, amount);
	}

	public int getSelectedValue() {
		return selectedValue;
	}
}
