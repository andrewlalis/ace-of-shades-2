package nl.andrewl.aos2_client.model;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.item.ItemTypes;
import nl.andrewl.aos_core.net.client.PlayerJoinMessage;
import org.joml.Matrix3f;
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

	private final Matrix4f modelTransform = new Matrix4f();
	private final float[] modelTransformData = new float[16];
	private final Matrix3f normalTransform = new Matrix3f();
	private final float[] normalTransformData = new float[9];

	private final Matrix4f heldItemTransform = new Matrix4f();
	private final float[] heldItemTransformData = new float[16];
	private final Matrix3f heldItemNormalTransform = new Matrix3f();
	private final float[] heldItemNormalTransformData = new float[9];

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

	public byte getSelectedBlockValue() {
		return selectedBlockValue;
	}

	public void setSelectedBlockValue(byte selectedBlockValue) {
		this.selectedBlockValue = selectedBlockValue;
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

		modelTransform.normal(normalTransform);
		normalTransform.get(normalTransformData);

		heldItemTransform.set(modelTransform)
				.translate(0.5f, 1.1f, -0.5f)
				.rotate((float) Math.PI, Camera.UP);
		heldItemTransform.get(heldItemTransformData);

		heldItemTransform.normal(heldItemNormalTransform);
		heldItemNormalTransform.get(heldItemNormalTransformData);
	}

	public float[] getModelTransformData() {
		return modelTransformData;
	}

	public float[] getNormalTransformData() {
		return normalTransformData;
	}

	public float[] getHeldItemTransformData() {
		return heldItemTransformData;
	}

	public float[] getHeldItemNormalTransformData() {
		return heldItemNormalTransformData;
	}

	public static OtherPlayer fromJoinMessage(PlayerJoinMessage msg, Client client) {
		OtherPlayer op = new OtherPlayer(msg.id(), msg.username());
		if (msg.teamId() != -1 && client.getTeams().containsKey(msg.teamId())) {
			op.setTeam(client.getTeams().get(msg.teamId()));
		}
		op.getPosition().set(msg.px(), msg.py(), msg.pz());
		op.getVelocity().set(msg.vx(), msg.vy(), msg.vz());
		op.getOrientation().set(msg.ox(), msg.oy());
		op.setHeldItemId(msg.selectedItemId());
		op.setSelectedBlockValue(msg.selectedBlockValue());
		op.setMode(msg.mode());
		return op;
	}
}
