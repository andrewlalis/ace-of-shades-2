package nl.andrewl.aos_core.model.item.gun;

import nl.andrewl.aos_core.model.item.Gun;

public class Ak47 extends Gun {
	public Ak47(int id) {
		super(
				id,
				"AK-47",
				4,
				30,
				1,
				0.95f,
				0.1f,
				1.2f,
				0.4f,
				0.1f,
				true
		);
	}
}
