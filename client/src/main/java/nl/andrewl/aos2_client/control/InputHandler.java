package nl.andrewl.aos2_client.control;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.Client;
import nl.andrewl.aos2_client.CommunicationHandler;
import nl.andrewl.aos2_client.control.context.ChattingContext;
import nl.andrewl.aos2_client.control.context.ExitMenuContext;
import nl.andrewl.aos2_client.control.context.NormalContext;

/**
 * Class which manages the player's input, and sending it to the server.
 */
public class InputHandler {
	private final Client client;
	private final CommunicationHandler comm;

	private long windowId;

	private final NormalContext normalContext;
	private final ChattingContext chattingContext;
	private final ExitMenuContext exitMenuContext;

	private InputContext activeContext;

	public InputHandler(Client client, CommunicationHandler comm, Camera cam) {
		this.client = client;
		this.comm = comm;
		this.normalContext = new NormalContext(this, cam);
		this.chattingContext = new ChattingContext(this);
		this.exitMenuContext = new ExitMenuContext(this);
		this.activeContext = normalContext;
	}

	public void setWindowId(long windowId) {
		this.windowId = windowId;
	}

	public InputContext getActiveContext() {
		return activeContext;
	}

	private void switchToContext(InputContext newContext) {
		if (newContext.equals(activeContext)) return;
		activeContext.onDisable();
		newContext.onEnable();
		activeContext = newContext;
	}

	public void switchToNormalContext() {
		switchToContext(normalContext);
	}

	public void switchToChattingContext() {
		switchToContext(chattingContext);
	}

	public void switchToExitMenuContext() {
		switchToContext(exitMenuContext);
	}

	public NormalContext getNormalContext() {
		return normalContext;
	}

	public ChattingContext getChattingContext() {
		return chattingContext;
	}

	public ExitMenuContext getExitMenuContext() {
		return exitMenuContext;
	}

	public boolean isNormalContextActive() {
		return normalContext.equals(activeContext);
	}

	public boolean isChattingContextActive() {
		return chattingContext.equals(activeContext);
	}

	public boolean isExitMenuContextActive() {
		return exitMenuContext.equals(activeContext);
	}

	public Client getClient() {
		return client;
	}

	public CommunicationHandler getComm() {
		return comm;
	}

	public long getWindowId() {
		return windowId;
	}
}
