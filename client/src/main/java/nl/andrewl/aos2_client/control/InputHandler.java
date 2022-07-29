package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos2_client.model.ClientPlayer;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.world.Hit;
import nl.andrewl.aos_core.net.client.BlockColorMessage;
import nl.andrewl.aos_core.net.client.ChatWrittenMessage;
import nl.andrewl.aos_core.net.client.ClientInputState;

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

	private boolean chatting;
	private StringBuffer chatText = new StringBuffer();


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
	}

	public boolean isForward() {
		return forward;
	}

	public void setForward(boolean forward) {
		if (chatting) return;
		this.forward = forward;
		updateInputState();
	}

	public boolean isBackward() {
		return backward;
	}

	public void setBackward(boolean backward) {
		if (chatting) return;
		this.backward = backward;
		updateInputState();
	}

	public boolean isLeft() {
		return left;
	}

	public void setLeft(boolean left) {
		if (chatting) return;
		this.left = left;
		updateInputState();
	}

	public boolean isRight() {
		return right;
	}

	public void setRight(boolean right) {
		if (chatting) return;
		this.right = right;
		updateInputState();
	}

	public boolean isJumping() {
		return jumping;
	}

	public void setJumping(boolean jumping) {
		if (chatting) return;
		this.jumping = jumping;
		updateInputState();
	}

	public boolean isCrouching() {
		return crouching;
	}

	public void setCrouching(boolean crouching) {
		if (chatting) return;
		this.crouching = crouching;
		updateInputState();
	}

	public boolean isSprinting() {
		return sprinting;
	}

	public void setSprinting(boolean sprinting) {
		if (chatting) return;
		this.sprinting = sprinting;
		updateInputState();
	}

	public boolean isHitting() {
		return hitting;
	}

	public void setHitting(boolean hitting) {
		if (chatting) return;
		this.hitting = hitting;
		updateInputState();
	}

	public boolean isInteracting() {
		return interacting;
	}

	public void setInteracting(boolean interacting) {
		if (chatting) return;
		this.interacting = interacting;
		updateInputState();
	}

	public boolean isReloading() {
		return reloading;
	}

	public void setReloading(boolean reloading) {
		if (chatting) return;
		this.reloading = reloading;
		updateInputState();
	}

	public int getSelectedInventoryIndex() {
		return selectedInventoryIndex;
	}

	public void setSelectedInventoryIndex(int selectedInventoryIndex) {
		if (chatting) return;
		this.selectedInventoryIndex = selectedInventoryIndex;
		updateInputState();
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void toggleDebugEnabled() {
		this.debugEnabled = !debugEnabled;
	}

	public void enableChatting() {
		if (chatting) return;
		setForward(false);
		setBackward(false);
		setLeft(false);
		setRight(false);
		setJumping(false);
		setCrouching(false);
		setSprinting(false);
		setReloading(false);
		chatting = true;
		chatText = new StringBuffer();
	}

	public boolean isChatting() {
		return chatting;
	}

	public void cancelChatting() {
		chatting = false;
		chatText.delete(0, chatText.length());
	}

	public void appendToChat(int codePoint) {
		if (!chatting || chatText.length() + 1 > 120) return;
		chatText.appendCodePoint(codePoint);
	}

	public void appendToChat(String s) {
		if (!chatting || chatText.length() + s.length() > 120) return;
		chatText.append(s);
	}

	public void deleteFromChat() {
		if (!chatting || chatText.length() == 0) return;
		chatText.deleteCharAt(chatText.length() - 1);
	}

	public String getChatText() {
		return chatText.toString();
	}

	public void sendChat() {
		if (!chatting) return;
		String text = chatText.toString().trim();
		cancelChatting();
		if (!text.isBlank()) {
			client.getCommunicationHandler().sendMessage(new ChatWrittenMessage(text));
		}
	}

	public boolean isScopeEnabled() {
		return interacting &&
				client.getMyPlayer().getInventory().getSelectedItemStack() instanceof GunItemStack;
	}

	public void pickBlock() {
		var player = client.getMyPlayer();
		if (player.getInventory().getSelectedItemStack() instanceof BlockItemStack stack) {
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
