package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.net.client.BlockColorMessage;
import org.lwjgl.glfw.GLFWScrollCallbackI;

public class PlayerInputMouseScrollCallback implements GLFWScrollCallbackI {
	private final Client client;
	private final CommunicationHandler comm;

	public PlayerInputMouseScrollCallback(Client client, CommunicationHandler comm) {
		this.client = client;
		this.comm = comm;
	}

	@Override
	public void invoke(long window, double xoffset, double yoffset) {
		System.out.println(yoffset);
		if (client.getMyPlayer().getInventory().getSelectedItemStack() instanceof BlockItemStack stack) {
			if (yoffset < 0) {
				stack.setSelectedValue((byte) (stack.getSelectedValue() - 1));
			} else if (yoffset > 0) {
				stack.setSelectedValue((byte) (stack.getSelectedValue() + 1));
			}
			comm.sendDatagramPacket(new BlockColorMessage(client.getMyPlayer().getId(), stack.getSelectedValue()));
		}
	}
}
