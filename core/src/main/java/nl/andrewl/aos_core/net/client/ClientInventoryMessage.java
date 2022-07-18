package nl.andrewl.aos_core.net.client;

import nl.andrewl.aos_core.model.item.Inventory;
import nl.andrewl.record_net.Message;

/**
 * A message that's sent by the server to a client with information about the
 * client's full inventory configuration. Here, we use a custom serializer
 * since the inventory object contains a lot of inheritance.
 *
 * @see InventorySerializer
 */
public record ClientInventoryMessage(
		Inventory inv
) implements Message {}
