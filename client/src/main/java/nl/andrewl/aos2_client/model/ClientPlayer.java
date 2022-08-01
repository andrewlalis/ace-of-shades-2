package nl.andrewl.aos2_client.model;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.Inventory;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class ClientPlayer extends Player {
	private final Inventory inventory;
	private float health;

	private final Matrix4f heldItemTransform = new Matrix4f();
	private final float[] heldItemTransformData = new float[16];

	private final Matrix3f heldItemNormalTransform = new Matrix3f();
	private final float[] heldItemNormalTransformData = new float[9];

	public ClientPlayer(int id, String username) {
		super(id, username);
		this.health = 1;
		this.inventory = new Inventory(new ArrayList<>(), 0);
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inv) {
		this.inventory.getItemStacks().clear();
		this.inventory.getItemStacks().addAll(inv.getItemStacks());
		this.inventory.setSelectedIndex(inv.getSelectedIndex());
	}

	public float getHealth() {
		return health;
	}

	public void setHealth(float health) {
		this.health = health;
	}

	public void updateHeldItemTransform(Camera cam, InputHandler inputHandler) {
		heldItemTransform.identity()
				.translate(cam.getPosition())
				.rotate((float) (cam.getOrientation().x + Math.PI), Camera.UP)
				.rotate(-cam.getOrientation().y + (float) Math.PI / 2, Camera.RIGHT);
		if (inputHandler.isNormalContextActive() && inputHandler.getNormalContext().isScopeEnabled()) {
			heldItemTransform.translate(0, -0.12f, 0);
		} else {
			heldItemTransform.translate(-0.35f, -0.4f, 0.5f);
		}
		heldItemTransform.get(heldItemTransformData);

		heldItemTransform.normal(heldItemNormalTransform);
		heldItemNormalTransform.get(heldItemNormalTransformData);
	}

	public float[] getHeldItemTransformData() {
		return heldItemTransformData;
	}

	public float[] getHeldItemNormalTransformData() {
		return heldItemNormalTransformData;
	}
}
