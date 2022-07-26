package nl.andrewl.aos_core.model.item.gun;

import nl.andrewl.aos_core.model.item.Gun;

public class Rifle extends Gun {
	public Rifle(int id) {
		super(
				id,
				"Rifle",
				5,
				8,
				1,
				0.97f,
				0.8f,
				2.5f,
				0.8f,
				50f,
				false
		);
	}
}
