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

	private ClientInputState lastInputState = null;

	public InputHandler(Client client, CommunicationHandler comm) {
		this.client = client;
		this.comm = comm;
	}

	public void updateInputState(long window) {
		// TODO: Allow customized keybindings.
		int selectedInventoryIndex;
		selectedInventoryIndex = client.getMyPlayer().getInventory().getSelectedIndex();
		if (glfwGetKey(window, GLFW_KEY_1) == GLFW_PRESS) selectedInventoryIndex = 0;
		if (glfwGetKey(window, GLFW_KEY_2) == GLFW_PRESS) selectedInventoryIndex = 1;
		if (glfwGetKey(window, GLFW_KEY_3) == GLFW_PRESS) selectedInventoryIndex = 2;

		ClientInputState currentInputState = new ClientInputState(
				comm.getClientId(),
				glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS,
				glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS,
				glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS,
				glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS,
				selectedInventoryIndex
		);
		if (!currentInputState.equals(lastInputState)) {
			comm.sendDatagramPacket(currentInputState);
			lastInputState = currentInputState;
		}

		ClientPlayer player = client.getMyPlayer();

		// Check for "pick block" functionality.
		if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_3) == GLFW_PRESS && player.getInventory().getSelectedItemStack() instanceof BlockItemStack stack) {
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
}
