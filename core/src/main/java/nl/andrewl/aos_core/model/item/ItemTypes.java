package nl.andrewl.aos_core.model.item;

import nl.andrewl.aos_core.model.item.gun.Ak47;
import nl.andrewl.aos_core.model.item.gun.Rifle;
import nl.andrewl.aos_core.model.item.gun.Winchester;

import java.util.HashMap;
import java.util.Map;

/**
 * Global constant set of registered item types.
 */
public final class ItemTypes {
	private static final Map<Integer, Item> TYPES_BY_ID = new HashMap<>();
	private static final Map<String, Item> TYPES_BY_NAME = new HashMap<>();

	public static final BlockItem BLOCK = new BlockItem(1);
	public static final Rifle RIFLE = new Rifle(2);
	public static final Ak47 AK_47 = new Ak47(3);
	public static final Winchester WINCHESTER = new Winchester(4);

	static {
		registerType(BLOCK);
		registerType(RIFLE);
		registerType(AK_47);
		registerType(WINCHESTER);
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
