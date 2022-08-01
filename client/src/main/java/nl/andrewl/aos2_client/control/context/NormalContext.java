package nl.andrewl.aos2_client.control.context;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.control.InputContext;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.util.WindowUtils;
import nl.andrewl.aos_core.model.item.BlockItemStack;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.item.ItemStack;
import nl.andrewl.aos_core.model.world.Hit;
import nl.andrewl.aos_core.net.client.BlockColorMessage;
import nl.andrewl.aos_core.net.client.ClientInputState;
import nl.andrewl.aos_core.net.client.ClientOrientationState;

import java.util.concurrent.ForkJoinPool;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The normal input context that occurs when the player is active in the game.
 * This includes moving around, interacting with their inventory, moving their
 * view, and so on.
 */
public class NormalContext implements InputContext {
	/**
	 * The number of milliseconds to wait before sending orientation updates,
	 * to prevent overloading the server.
	 */
	private static final int ORIENTATION_UPDATE_LIMIT = 20;

	private final InputHandler inputHandler;
	private final Camera camera;

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

	private float lastMouseCursorX;
	private float lastMouseCursorY;
	private long lastOrientationUpdateSentAt = 0L;

	public NormalContext(InputHandler inputHandler, Camera camera) {
		this.inputHandler = inputHandler;
		this.camera = camera;
	}

	public void updateInputState() {
		var comm = inputHandler.getComm();
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
	}

	public void resetInputState() {
		forward = false;
		backward = false;
		left = false;
		right = false;
		jumping = false;
		crouching = false;
		sprinting = false;
		hitting = false;
		interacting = false;
		reloading = false;
		var size = WindowUtils.getSize(inputHandler.getWindowId());
		lastMouseCursorX = size.first() / 2f;
		lastMouseCursorY = size.second() / 2f;
		updateInputState();
	}

	public void setForward(boolean forward) {
		this.forward = forward;
		updateInputState();
	}

	public void setBackward(boolean backward) {
		this.backward = backward;
		updateInputState();
	}

	public void setLeft(boolean left) {
		this.left = left;
		updateInputState();
	}

	public void setRight(boolean right) {
		this.right = right;
		updateInputState();
	}

	public void setJumping(boolean jumping) {
		this.jumping = jumping;
		updateInputState();
	}

	public void setCrouching(boolean crouching) {
		this.crouching = crouching;
		updateInputState();
	}

	public void setSprinting(boolean sprinting) {
		this.sprinting = sprinting;
		updateInputState();
	}

	public void setHitting(boolean hitting) {
		this.hitting = hitting;
		updateInputState();
	}

	public void setInteracting(boolean interacting) {
		this.interacting = interacting;
		updateInputState();
	}

	public void setReloading(boolean reloading) {
		this.reloading = reloading;
		updateInputState();
	}

	public void setSelectedInventoryIndex(int selectedInventoryIndex) {
		this.selectedInventoryIndex = selectedInventoryIndex;
		updateInputState();
	}

	public void toggleDebugEnabled() {
		this.debugEnabled = !debugEnabled;
	}

	public void enableChatting() {
		inputHandler.switchToChattingContext();
	}

	@Override
	public void onEnable() {
		resetInputState();
	}

	@Override
	public void onDisable() {
		resetInputState();
	}

	@Override
	public void keyPress(long window, int key, int mods) {
		switch (key) {
			case GLFW_KEY_W -> setForward(true);
			case GLFW_KEY_A -> setLeft(true);
			case GLFW_KEY_S -> setBackward(true);
			case GLFW_KEY_D -> setRight(true);
			case GLFW_KEY_SPACE -> setJumping(true);
			case GLFW_KEY_LEFT_CONTROL -> setCrouching(true);
			case GLFW_KEY_LEFT_SHIFT -> setSprinting(true);
			case GLFW_KEY_R -> setReloading(true);

			case GLFW_KEY_1 -> setSelectedInventoryIndex(0);
			case GLFW_KEY_2 -> setSelectedInventoryIndex(1);
			case GLFW_KEY_3 -> setSelectedInventoryIndex(2);
			case GLFW_KEY_4 -> setSelectedInventoryIndex(3);

			case GLFW_KEY_F3 -> toggleDebugEnabled();
			case GLFW_KEY_ESCAPE -> inputHandler.switchToExitMenuContext();
		}
	}

	@Override
	public void keyRelease(long window, int key, int mods) {
		switch (key) {
			case GLFW_KEY_W -> setForward(false);
			case GLFW_KEY_A -> setLeft(false);
			case GLFW_KEY_S -> setBackward(false);
			case GLFW_KEY_D -> setRight(false);
			case GLFW_KEY_SPACE -> setJumping(false);
			case GLFW_KEY_LEFT_CONTROL -> setCrouching(false);
			case GLFW_KEY_LEFT_SHIFT -> setSprinting(false);
			case GLFW_KEY_R -> setReloading(false);

			case GLFW_KEY_T -> enableChatting();
			case GLFW_KEY_SLASH -> {
				enableChatting();
				inputHandler.getChattingContext().appendToChat("/");
			}
		}
	}

	@Override
	public void mouseButtonPress(long window, int button, int mods) {
		switch (button) {
			case GLFW_MOUSE_BUTTON_1 -> setHitting(true);
			case GLFW_MOUSE_BUTTON_2 -> setInteracting(true);
			case GLFW_MOUSE_BUTTON_3 -> pickBlock();
		}
	}

	@Override
	public void mouseButtonRelease(long window, int button, int mods) {
		switch (button) {
			case GLFW_MOUSE_BUTTON_1 -> setHitting(false);
			case GLFW_MOUSE_BUTTON_2 -> setInteracting(false);
		}
	}

	@Override
	public void mouseScroll(long window, double xOffset, double yOffset) {
		var player = inputHandler.getClient().getMyPlayer();
		ItemStack stack = player.getInventory().getSelectedItemStack();
		if (stack instanceof BlockItemStack blockStack) {
			if (yOffset < 0) {
				blockStack.setSelectedValue((byte) (blockStack.getSelectedValue() - 1));
			} else if (yOffset > 0) {
				blockStack.setSelectedValue((byte) (blockStack.getSelectedValue() + 1));
			}
			inputHandler.getComm().sendDatagramPacket(new BlockColorMessage(player.getId(), blockStack.getSelectedValue()));
		}
	}

	@Override
	public void mouseCursorPos(long window, double xPos, double yPos) {
		double[] xb = new double[1];
		double[] yb = new double[1];
		glfwGetCursorPos(window, xb, yb);
		float x = (float) xb[0];
		float y = (float) yb[0];
		float dx = x - lastMouseCursorX;
		float dy = y - lastMouseCursorY;
		lastMouseCursorX = x;
		lastMouseCursorY = y;
		var client = inputHandler.getClient();
		float trueSensitivity = inputHandler.getClient().getConfig().input.mouseSensitivity;
		if (isScopeEnabled()) trueSensitivity *= 0.1f;
		client.getMyPlayer().setOrientation(
				client.getMyPlayer().getOrientation().x - dx * trueSensitivity,
				client.getMyPlayer().getOrientation().y - dy * trueSensitivity
		);
		camera.setOrientationToPlayer(client.getMyPlayer());
		long now = System.currentTimeMillis();
		if (lastOrientationUpdateSentAt + ORIENTATION_UPDATE_LIMIT < now) {
			ForkJoinPool.commonPool().submit(() -> inputHandler.getComm().sendDatagramPacket(ClientOrientationState.fromPlayer(client.getMyPlayer())));
			lastOrientationUpdateSentAt = now;
		}
	}

	public void pickBlock() {
		var client = inputHandler.getClient();
		var comm = inputHandler.getComm();
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

	public boolean isScopeEnabled() {
		return interacting &&
				inputHandler.getClient().getMyPlayer().getInventory().getSelectedItemStack() instanceof GunItemStack;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}
}
