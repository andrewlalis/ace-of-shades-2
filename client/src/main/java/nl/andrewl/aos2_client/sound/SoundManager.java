package nl.andrewl.aos2_client.sound;

import nl.andrewl.aos_core.model.Player;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * Main class for managing the OpenAL audio interface.
 */
public class SoundManager {
	private static final Logger log = LoggerFactory.getLogger(SoundManager.class);

	private static final int SOURCE_COUNT = 32;

	private final long alContext;
	private final Map<String, Integer> audioBuffers = new HashMap<>();

	/**
	 * A set of pre-allocated sound sources that can be utilized when short
	 * sounds need to be played.
	 */
	private final List<SoundSource> availableSources = new ArrayList<>(SOURCE_COUNT);

	private final Map<Player, Long> lastPlayerWalkingSounds = new ConcurrentHashMap<>();

	public SoundManager() {
		long device = alcOpenDevice((ByteBuffer) null);
		ALCCapabilities capabilities = ALC.createCapabilities(device);
		alContext = alcCreateContext(device, (IntBuffer) null);
		alcMakeContextCurrent(alContext);
		AL.createCapabilities(capabilities);

		alDistanceModel(AL_EXPONENT_DISTANCE);

		for (int i = 0; i < SOURCE_COUNT; i++) {
			SoundSource source = new SoundSource();
			alSourcef(source.getId(), AL_ROLLOFF_FACTOR, 1);
			alSourcef(source.getId(), AL_REFERENCE_DISTANCE, 10);
			alSourcef(source.getId(), AL_MAX_DISTANCE, 20);
			availableSources.add(source);
		}

		loadSounds();
	}

	private void loadSounds() {
		load("footsteps_1", "sound/m_footsteps_1.wav");
		load("footsteps_2", "sound/m_footsteps_2.wav");
		load("footsteps_3", "sound/m_footsteps_3.wav");
		load("footsteps_4", "sound/m_footsteps_4.wav");
		load("shot_m1-garand_1", "sound/m_shot_m1-garand_1.wav");
		load("shot_ak-47_1", "sound/m_shot_ak-47_1.wav");
		load("shot_winchester_1", "sound/m_shot_winchester_1.wav");
		load("bullet_impact_1", "sound/m_bullet_impact_1.wav");
		load("bullet_impact_2", "sound/m_bullet_impact_2.wav");
		load("bullet_impact_3", "sound/m_bullet_impact_3.wav");
		load("bullet_impact_4", "sound/m_bullet_impact_4.wav");
		load("bullet_impact_5", "sound/m_bullet_impact_5.wav");
		load("reload", "sound/m_reload.wav");
		load("death", "sound/m_death.wav");
		load("hurt_1", "sound/m_hurt_1.wav");
		load("hurt_2", "sound/m_hurt_2.wav");
		load("hurt_3", "sound/m_hurt_3.wav");
		load("block_break_1", "sound/m_block_break_1.wav");
		load("block_place_1", "sound/m_block_place_1.wav");
		load("chat", "sound/chat.wav");
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

	public Integer getSoundBuffer(String name) {
		return audioBuffers.get(name);
	}

	public void play(String soundName, float gain, Vector3f position, Vector3f velocity) {
		Integer bufferId = getSoundBuffer(soundName);
		if (bufferId == null) {
			log.warn("Attempted to play unknown sound \"{}\"", soundName);
		} else {
			SoundSource source = getNextAvailableSoundSource();
			if (source != null) {
				source.setPosition(position);
				source.setVelocity(velocity);
				source.setGain(gain);
				source.play(bufferId);
			} else {
				log.warn("Couldn't get an available sound source to play sound \"{}\"", soundName);
			}
		}
	}

	public void play(String soundName, float gain, Vector3f position) {
		play(soundName, gain, position, new Vector3f(0, 0, 0));
	}

	public void playWalkingSounds(Player player, long now) {
		if (player.getVelocity().length() <= 0) return;
		long lastSoundAt = lastPlayerWalkingSounds.computeIfAbsent(player, p -> 0L);
		long delay = 500; // Delay in ms between footfalls.
		if (player.getVelocity().length() > 5) delay -= 150;
		if (player.getVelocity().length() < 3) delay += 150;
		if (lastSoundAt + delay < now) {
			int choice = ThreadLocalRandom.current().nextInt(1, 5);
			play("footsteps_" + choice, 0.5f, player.getPosition(), player.getVelocity());
			lastPlayerWalkingSounds.put(player, now);
		}
	}

	public Collection<SoundSource> getSources() {
		return Collections.unmodifiableCollection(availableSources);
	}

	private SoundSource getNextAvailableSoundSource() {
		for (var source : availableSources) {
			if (!source.isPlaying()) return source;
		}
		return null;
	}

	public void free() {
		for (var bufferId : audioBuffers.values()) {
			alDeleteBuffers(bufferId);
		}
		for (var source : availableSources) {
			source.free();
		}
		alcDestroyContext(alContext);
	}
}
