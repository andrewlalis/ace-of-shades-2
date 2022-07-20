package nl.andrewl.aos2_client.model;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.Inventory;
import org.joml.Matrix4f;

import java.util.ArrayList;

public class ClientPlayer extends Player {
	private final Inventory inventory;

	private final Matrix4f heldItemTransform = new Matrix4f();
	private final float[] heldItemTransformData = new float[16];

	public ClientPlayer(int id, String username) {
		super(id, username);
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

	public void updateHeldItemTransform(Camera cam) {
		heldItemTransform.identity()
				.translate(cam.getPosition())
				.rotate((float) (cam.getOrientation().x + Math.PI), Camera.UP)
				.rotate(-cam.getOrientation().y + (float) Math.PI / 2, Camera.RIGHT)
				.translate(-0.35f, -0.4f, 0.5f);
		heldItemTransform.get(heldItemTransformData);
	}

	public float[] getHeldItemTransformData() {
		return heldItemTransformData;
	}
}
