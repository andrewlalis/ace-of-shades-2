package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.net.udp.ClientInputState;

public class ServerPlayer extends Player {
	private ClientInputState lastInputState;

	public ServerPlayer(int id, String username) {
		super(id, username);
		// Initialize with a default state of no input.
		lastInputState = new ClientInputState(id, false, false, false, false, false, false, false);
	}

	public ClientInputState getLastInputState() {
		return lastInputState;
	}

	public void setLastInputState(ClientInputState inputState) {
		this.lastInputState = inputState;
	}
}
