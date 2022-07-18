package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.record_net.MessageReader;
import nl.andrewl.record_net.MessageTypeSerializer;
import nl.andrewl.record_net.MessageWriter;

import java.util.function.Function;

public class ItemStackSerializer implements MessageTypeSerializer<ItemStackMessage> {
	@Override
	public Class<ItemStackMessage> messageClass() {
		return ItemStackMessage.class;
	}

	@Override
	public Function<ItemStackMessage, Integer> byteSizeFunction() {
		return msg -> Integer.BYTES + ItemStack.byteSize(msg.stack());
	}

	@Override
	public MessageReader<ItemStackMessage> reader() {
		return in -> {
			int index = in.readInt();
			ItemStack stack = ItemStack.read(in);
			return new ItemStackMessage(index, stack);
		};
	}

	@Override
	public MessageWriter<ItemStackMessage> writer() {
		return (msg, out) -> {
			out.writeInt(msg.index());
			ItemStack.write(msg.stack(), out);
		};
	}
}
