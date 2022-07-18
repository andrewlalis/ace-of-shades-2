package nl.andrewl.aos2_client.model;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.ItemTypes;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * An extension of the player class with only the information needed to display
 * other players in the game, without needing to hold any sensitive info.
 */
public class OtherPlayer extends Player {
	/**
	 * The item id of this player's held item.
	 */
	private int heldItemId;

	/**
	 * The value of block that the player has selected. We use this to show
	 * what color of block the player is holding.
	 */
	private byte selectedBlockValue;

	/**
	 * The transformation used to render this player in the world.
	 */
	private final Matrix4f modelTransform = new Matrix4f();
	private final float[] modelTransformData = new float[16];

	private final Matrix4f heldItemTransform = new Matrix4f();
	private final float[] heldItemTransformData = new float[16];

	public OtherPlayer(int id, String username) {
		super(id, username);
		this.heldItemId = ItemTypes.RIFLE.getId();
	}

	public int getHeldItemId() {
		return heldItemId;
	}

	public void setHeldItemId(int heldItemId) {
		this.heldItemId = heldItemId;
	}

	@Override
	public void setPosition(Vector3f position) {
		super.setPosition(position);
		updateModelTransform();
	}

	@Override
	public void setOrientation(float x, float y) {
		super.setOrientation(x, y);
		updateModelTransform();
	}

	public void updateModelTransform() {
		modelTransform.identity()
				.translate(position)
				.rotate(orientation.x, Camera.UP);
		modelTransform.get(modelTransformData);
		heldItemTransform.set(modelTransform)
				.translate(0.5f, 1.1f, -0.5f)
				.rotate((float) Math.PI, Camera.UP);
		heldItemTransform.get(heldItemTransformData);
	}

	public float[] getModelTransformData() {
		return modelTransformData;
	}

	public float[] getHeldItemTransformData() {
		return heldItemTransformData;
	}
}
