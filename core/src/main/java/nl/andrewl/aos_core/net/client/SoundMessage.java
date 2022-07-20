package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that indicates that a sound has been emitted somewhere in the
 * world, and that clients may need to play the sound.
 */
public record SoundMessage(
		String name,
		float px, float py, float pz
) implements Message {}
