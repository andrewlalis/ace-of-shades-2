package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.net.client.ClientInputState;

/**
 * Wrapper around the various information we have about a player's input state,
 * including their last known state, and any impulses they've made since the
 * last tick.
 */
public class PlayerInputTracker {
	private ClientInputState lastInputState;
	private final PlayerImpulses impulsesSinceLastTick;
	
	public PlayerInputTracker(ServerPlayer player) {
		lastInputState = new ClientInputState(
				player.getId(),
				false, false, false, false,
				false, false, false,
				false, false, false,
				player.getInventory().getSelectedIndex()
		);
		this.impulsesSinceLastTick = new PlayerImpulses();
	}

	public boolean setLastInputState(ClientInputState lastInputState) {
		boolean updated = !lastInputState.equals(this.lastInputState);
		if (updated) {
			this.lastInputState = lastInputState;
			impulsesSinceLastTick.update(lastInputState);
		}
		return updated;
	}

	public void reset() {
		impulsesSinceLastTick.reset();
	}
	
	public boolean forward() {
		return lastInputState.forward() || impulsesSinceLastTick.forward;
	}
	
	public boolean backward() {
		return lastInputState.backward() || impulsesSinceLastTick.backward;
	}
	
	public boolean left() {
		return lastInputState.left() || impulsesSinceLastTick.left;
	}
	
	public boolean right() {
		return lastInputState.right() || impulsesSinceLastTick.right;
	}
	
	public boolean jumping() {
		return lastInputState.jumping() || impulsesSinceLastTick.jumping;
	}

	public boolean crouching() {
		return lastInputState.crouching() || impulsesSinceLastTick.crouching;
	}

	public boolean sprinting() {
		return lastInputState.sprinting() || impulsesSinceLastTick.sprinting;
	}

	public boolean hitting() {
		return lastInputState.hitting() || impulsesSinceLastTick.hitting;
	}

	public boolean interacting() {
		return lastInputState.interacting() || impulsesSinceLastTick.interacting;
	}

	public boolean reloading() {
		return lastInputState.reloading() || impulsesSinceLastTick.reloading;
	}

	public int selectedInventoryIndex() {
		return lastInputState.selectedInventoryIndex();
	}
}
