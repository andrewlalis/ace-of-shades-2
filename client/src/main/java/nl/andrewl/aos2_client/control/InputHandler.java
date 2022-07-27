package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.world.Hit;
import nl.andrewl.aos_core.net.client.BlockColorMessage;
import nl.andrewl.aos_core.net.client.ClientInputState;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Class which manages the player's input, and sending it to the server.
 */
public class InputHandler {
	private final Client client;
	private final CommunicationHandler comm;

	private long windowId;

	private ClientInputState lastInputState = null;

	private boolean forward;
	private boolean backward;
	private boolean left;
	private boolean right;
	private boolean jumping;
	private boolean crouching;
	private boolean sprinting;
	private boolean hitting;
	private boolean interacting;
	private boolean reloading;
	private int selectedInventoryIndex;

	private boolean debugEnabled;


	public InputHandler(Client client, CommunicationHandler comm) {
		this.client = client;
		this.comm = comm;
	}

	public void setWindowId(long windowId) {
		this.windowId = windowId;
	}

	public void updateInputState() {
		ClientInputState currentInputState = new ClientInputState(
				comm.getClientId(),
				forward, backward, left, right,
				jumping, crouching, sprinting,
				hitting, interacting, reloading,
				selectedInventoryIndex
		);
		if (!currentInputState.equals(lastInputState)) {
			comm.sendDatagramPacket(currentInputState);
			lastInputState = currentInputState;
		}

		ClientPlayer player = client.getMyPlayer();

		// Check for "pick block" functionality.
		if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_3) == GLFW_PRESS && player.getInventory().getSelectedItemStack() instanceof BlockItemStack stack) {
			Hit hit = client.getWorld().getLookingAtPos(player.getEyePosition(), player.getViewVector(), 50);
			if (hit != null) {
				byte selectedBlock = client.getWorld().getBlockAt(hit.pos().x, hit.pos().y, hit.pos().z);
				if (selectedBlock > 0) {
					stack.setSelectedValue(selectedBlock);
					comm.sendDatagramPacket(new BlockColorMessage(player.getId(), selectedBlock));
				}
			}
		}
	}

	public boolean isForward() {
		return forward;
	}

	public void setForward(boolean forward) {
		this.forward = forward;
		updateInputState();
	}

	public boolean isBackward() {
		return backward;
	}

	public void setBackward(boolean backward) {
		this.backward = backward;
		updateInputState();
	}

	public boolean isLeft() {
		return left;
	}

	public void setLeft(boolean left) {
		this.left = left;
		updateInputState();
	}

	public boolean isRight() {
		return right;
	}

	public void setRight(boolean right) {
		this.right = right;
		updateInputState();
	}

	public boolean isJumping() {
		return jumping;
	}

	public void setJumping(boolean jumping) {
		this.jumping = jumping;
		updateInputState();
	}

	public boolean isCrouching() {
		return crouching;
	}

	public void setCrouching(boolean crouching) {
		this.crouching = crouching;
		updateInputState();
	}

	public boolean isSprinting() {
		return sprinting;
	}

	public void setSprinting(boolean sprinting) {
		this.sprinting = sprinting;
		updateInputState();
	}

	public boolean isHitting() {
		return hitting;
	}

	public void setHitting(boolean hitting) {
		this.hitting = hitting;
		updateInputState();
	}

	public boolean isInteracting() {
		return interacting;
	}

	public void setInteracting(boolean interacting) {
		this.interacting = interacting;
		updateInputState();
	}

	public boolean isReloading() {
		return reloading;
	}

	public void setReloading(boolean reloading) {
		this.reloading = reloading;
		updateInputState();
	}

	public int getSelectedInventoryIndex() {
		return selectedInventoryIndex;
	}

	public void setSelectedInventoryIndex(int selectedInventoryIndex) {
		this.selectedInventoryIndex = selectedInventoryIndex;
		updateInputState();
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void toggleDebugEnabled() {
		this.debugEnabled = !debugEnabled;
	}
}
