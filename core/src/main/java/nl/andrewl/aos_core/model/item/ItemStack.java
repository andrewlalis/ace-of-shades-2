package nl.andrewl.aos_core.model.item;

import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;

import java.io.IOException;

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
		if (amount > -1) {
			this.amount = amount;
		}
	}

	public void incrementAmount() {
		setAmount(amount + 1);
	}

	public void decrementAmount() {
		setAmount(amount - 1);
	}

	public static int byteSize(ItemStack stack) {
		int bytes = 2 * Integer.BYTES;
		if (stack instanceof BlockItemStack) {
			bytes += Byte.BYTES;
		} else if (stack instanceof GunItemStack) {
			bytes += 2 * Integer.BYTES;
		}
		return bytes;
	}

	public static void write(ItemStack stack, ExtendedDataOutputStream out) throws IOException {
		out.writeInt(stack.type.id);
		out.writeInt(stack.amount);
		if (stack instanceof BlockItemStack b) {
			out.writeByte(b.getSelectedValue());
		} else if (stack instanceof GunItemStack g) {
			out.writeInt(g.getClipCount());
			out.writeInt(g.getBulletCount());
		}
	}

	public static ItemStack read(ExtendedDataInputStream in) throws IOException {
		int typeId = in.readInt();
		Item item = ItemTypes.get(typeId);
		int amount = in.readInt();
		if (item instanceof Gun g) {
			int clipCount = in.readInt();
			int bulletCount = in.readInt();
			return new GunItemStack(g, bulletCount, clipCount);
		} else if (item instanceof BlockItem b) {
			byte selectedValue = in.readByte();
			return new BlockItemStack(b, amount, selectedValue);
		} else {
			throw new IOException("Invalid item stack.");
		}
	}
}
