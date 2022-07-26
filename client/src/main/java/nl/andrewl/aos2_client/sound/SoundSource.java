package nl.andrewl.aos2_client.sound;

import org.joml.Vector3f;

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

	public void setPosition(Vector3f pos) {
		alSource3f(sourceId, AL_POSITION, pos.x, pos.y, pos.z);
	}

	public void setVelocity(Vector3f vel) {
		alSource3f(sourceId, AL_VELOCITY, vel.x, vel.y, vel.z);
	}

	public void setDirection(Vector3f dir) {
		alSource3f(sourceId, AL_DIRECTION, dir.x, dir.y, dir.z);
	}

	public void setGain(float gain) {
		alSourcef(sourceId, AL_GAIN, gain);
	}

	public int getId() {
		return sourceId;
	}

	public boolean isPlaying() {
		return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
	}

	public void free() {
		alDeleteSources(sourceId);
	}
}
