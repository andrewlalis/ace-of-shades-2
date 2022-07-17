package nl.andrewl.aos_core.model.item;

import nl.andrewl.aos_core.model.item.gun.Rifle;

import java.util.HashMap;
import java.util.Map;

/**
 * Global constant set of registered item types.
 */
public final class ItemTypes {
	private static final Map<Integer, Item> TYPES_BY_ID = new HashMap<>();
	private static final Map<String, Item> TYPES_BY_NAME = new HashMap<>();

	static {
		registerType(new BlockItem(1));
		registerType(new Rifle(2));
	}

	public static void registerType(Item type) {
		TYPES_BY_ID.put(type.getId(), type);
		TYPES_BY_NAME.put(type.getName(), type);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Item> T get(int id) {
		return (T) TYPES_BY_ID.get(id);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Item> T get(String name) {
		return (T) TYPES_BY_NAME.get(name);
	}
}
