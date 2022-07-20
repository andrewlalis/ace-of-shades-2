package nl.andrewl.aos2_client.sound;

import static org.lwjgl.openal.AL10.*;

public class SoundSource {
	private final int sourceId;

	public SoundSource() {
		sourceId = alGenSources();
		alSourcef(sourceId, AL_GAIN, 1);
		alSourcef(sourceId, AL_PITCH, 1);
		alSource3f(sourceId, AL_POSITION, 0, 0, 0);
	}

	public void play(int bufferId) {
		alSourcei(sourceId, AL_BUFFER, bufferId);
		alSourcePlay(sourceId);
	}

	public void free() {
		alDeleteSources(sourceId);
	}
}
