package nl.andrewl.aos_core.model.item;

public class BlockItemStack extends ItemStack {
	private byte selectedValue = 1;

	public BlockItemStack(BlockItem item, int amount, byte selectedValue) {
		super(item, amount);
		this.selectedValue = selectedValue;
	}

	public BlockItemStack(BlockItem item) {
		this(item, 50, (byte) 1);
	}

	public byte getSelectedValue() {
		return selectedValue;
	}

	public void setSelectedValue(byte selectedValue) {
		if (selectedValue < 1) return;
		this.selectedValue = selectedValue;
	}
}
