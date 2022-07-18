package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.item.Inventory;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.record_net.MessageReader;
import nl.andrewl.record_net.MessageTypeSerializer;
import nl.andrewl.record_net.MessageWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class InventorySerializer implements MessageTypeSerializer<ClientInventoryMessage> {
	@Override
	public Class<ClientInventoryMessage> messageClass() {
		return ClientInventoryMessage.class;
	}

	@Override
	public Function<ClientInventoryMessage, Integer> byteSizeFunction() {
		return msg -> {
			int bytes = Integer.BYTES; // For the stack count size.
			for (var stack : msg.inv().getItemStacks()) {
				bytes += ItemStack.byteSize(stack);
			}
			bytes += Integer.BYTES; // Selected index.
			return bytes;
		};
	}

	@Override
	public MessageReader<ClientInventoryMessage> reader() {
		return in -> {
			int stacksCount = in.readInt();
			List<ItemStack> stacks = new ArrayList<>(stacksCount);
			for (int i = 0; i < stacksCount; i++) {
				stacks.add(ItemStack.read(in));
			}
			int selectedIndex = in.readInt();
			Inventory inv = new Inventory(stacks, selectedIndex);
			return new ClientInventoryMessage(inv);
		};
	}

	@Override
	public MessageWriter<ClientInventoryMessage> writer() {
		return (msg, out) -> {
			Inventory inv = msg.inv();
			out.writeInt(inv.getItemStacks().size());
			for (var stack : inv.getItemStacks()) {
				ItemStack.write(stack, out);
			}
			out.writeInt(inv.getSelectedIndex());
		};
	}
}
