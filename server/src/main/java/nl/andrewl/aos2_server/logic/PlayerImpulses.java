package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos_core.net.client.ClientInputState;

public class PlayerImpulses {
	public boolean forward;
	public boolean backward;
	public boolean left;
	public boolean right;
	public boolean jumping;
	public boolean crouching;
	public boolean sprinting;
	public boolean hitting;
	public boolean interacting;
	public boolean reloading;
	
	public void update(ClientInputState s) {
		forward = forward || s.forward();
		backward = backward || s.backward();
		left = left || s.left();
		right = right || s.right();
		jumping = jumping || s.jumping();
		crouching = crouching || s.crouching();
		sprinting = sprinting || s.sprinting();
		hitting = hitting || s.hitting();
		interacting = interacting || s.interacting();
		reloading = reloading || s.reloading();
	}
	
	public void reset() {
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
	}
}
