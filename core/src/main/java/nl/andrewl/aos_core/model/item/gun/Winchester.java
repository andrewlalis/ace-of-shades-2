package nl.andrewl.aos_core.model.item.gun;

import nl.andrewl.aos_core.model.item.Gun;

public class Winchester extends Gun {
	public Winchester(int id) {
		super(
				id,
				"Winchester",
				6,
				4,
				4,
				0.85f,
				0.75f,
				2.5f,
				0.3f,
				0.33f,
				false
		);
	}
}
