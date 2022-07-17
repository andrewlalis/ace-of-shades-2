package nl.andrewl.aos_core.model.item;

import java.util.HashMap;
import java.util.Map;

/**
 * Global constant set of registered item types.
 */
public final class ItemTypes {
	public static final Map<Integer, ItemType> TYPES_MAP = new HashMap<>();

	static {
		registerType(new ItemType(1, "Rifle", 1));
		registerType(new ItemType(2, "Block", 100));
	}

	public static void registerType(ItemType type) {
		TYPES_MAP.put(type.getId(), type);
	}
}
