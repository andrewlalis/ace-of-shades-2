package nl.andrewl.aos2_client.sound;

import nl.andrewl.aos2_client.model.ClientPlayer;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.AL10.*;

public class SoundManager {
	private final long alContext;
	private final Map<String, Integer> audioBuffers = new HashMap<>();

	public SoundManager() {
		long device = alcOpenDevice((ByteBuffer) null);
		ALCCapabilities capabilities = ALC.createCapabilities(device);
		alContext = alcCreateContext(device, (IntBuffer) null);
		alcMakeContextCurrent(alContext);
		AL.createCapabilities(capabilities);

	}

	public void load(String name, String resource) {
		int bufferId = alGenBuffers();
		audioBuffers.put(name, bufferId);
		WaveData waveData = WaveData.create(resource);
		alBufferData(bufferId, waveData.format, waveData.data, waveData.samplerate);
		waveData.dispose();
	}

	public void updateListener(Vector3f position, Vector3f velocity) {
		alListener3f(AL_POSITION, position.x(), position.y(), position.z());
		alListener3f(AL_VELOCITY, velocity.x(), velocity.y(), velocity.z());
	}

	public int getSoundBuffer(String name) {
		return audioBuffers.get(name);
	}

	public void free() {
		for (var bufferId : audioBuffers.values()) {
			alDeleteBuffers(bufferId);
		}
		alcDestroyContext(alContext);
	}
}
