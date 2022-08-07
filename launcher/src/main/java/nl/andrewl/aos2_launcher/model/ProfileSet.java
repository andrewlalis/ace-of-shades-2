package nl.andrewl.aos2_launcher.model;

import com.google.gson.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import nl.andrewl.aos2_launcher.Launcher;
import nl.andrewl.aos2_launcher.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Model for managing the set of profiles in the app.
 */
public class ProfileSet {
	private final ObservableList<Profile> profiles;
	private final ObjectProperty<Profile> selectedProfile;
	private Path lastFileUsed = null;

	public ProfileSet() {
		this.profiles = FXCollections.observableArrayList();
		this.selectedProfile = new SimpleObjectProperty<>(null);
	}

	public ProfileSet(Path file) throws IOException {
		this();
		load(file);
	}

	public void addNewProfile(Profile profile) {
		profiles.add(profile);
		save();
		try {
			if (!Files.exists(profile.getDir())) {
				Files.createDirectory(profile.getDir());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void removeProfile(Profile profile) {
		if (profile == null) return;
		boolean removed = profiles.remove(profile);
		if (removed) {
			try {
				if (Files.exists(profile.getDir())) {
					FileUtils.deleteRecursive(profile.getDir());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			save();
		}
	}

	public void removeSelectedProfile() {
		removeProfile(getSelectedProfile());
	}

	public void load(Path file) throws IOException {
		try (var reader = Files.newBufferedReader(file)) {
			JsonObject data = new Gson().fromJson(reader, JsonObject.class);
			profiles.clear();
			JsonElement selectedProfileIdElement = data.get("selectedProfileId");
			UUID selectedProfileId = (selectedProfileIdElement == null || selectedProfileIdElement.isJsonNull())
					? null
					: UUID.fromString(selectedProfileIdElement.getAsString());
			JsonArray profilesArray = data.getAsJsonArray("profiles");
			for (JsonElement element : profilesArray) {
				JsonObject profileObj = element.getAsJsonObject();
				UUID id = UUID.fromString(profileObj.get("id").getAsString());
				String name = profileObj.get("name").getAsString();
				String clientVersion = profileObj.get("clientVersion").getAsString();
				String username = profileObj.get("username").getAsString();
				JsonElement jvmArgsElement = profileObj.get("jvmArgs");
				String jvmArgs = null;
				if (jvmArgsElement != null && jvmArgsElement.isJsonPrimitive() && jvmArgsElement.getAsJsonPrimitive().isString()) {
					jvmArgs = jvmArgsElement.getAsString();
				}
				Profile profile = new Profile(id, name, username, clientVersion, jvmArgs);
				profiles.add(profile);
				if (selectedProfileId != null && selectedProfileId.equals(profile.getId())) {
					selectedProfile.set(profile);
				}
			}
			lastFileUsed = file;
		}
	}

	public void loadOrCreateStandardFile() {
		if (!Files.exists(Launcher.PROFILES_FILE)) {
			try {
				save(Launcher.PROFILES_FILE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			try {
				load(Launcher.PROFILES_FILE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public void save(Path file) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject data = new JsonObject();
		String selectedProfileId = selectedProfile.getValue() == null ? null : selectedProfile.getValue().getId().toString();
		data.addProperty("selectedProfileId", selectedProfileId);
		JsonArray profilesArray = new JsonArray(profiles.size());
		for (Profile profile : profiles) {
			JsonObject obj = new JsonObject();
			obj.addProperty("id", profile.getId().toString());
			obj.addProperty("name", profile.getName());
			obj.addProperty("username", profile.getUsername());
			obj.addProperty("clientVersion", profile.getClientVersion());
			obj.addProperty("jvmArgs", profile.getJvmArgs());
			profilesArray.add(obj);
		}
		data.add("profiles", profilesArray);
		try (var writer = Files.newBufferedWriter(file)) {
			gson.toJson(data, writer);
		}
		lastFileUsed = file;
	}

	public void save() {
		if (lastFileUsed != null) {
			try {
				save(lastFileUsed);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public ObservableList<Profile> getProfiles() {
		return profiles;
	}

	public Profile getSelectedProfile() {
		return selectedProfile.get();
	}

	public ObjectProperty<Profile> selectedProfileProperty() {
		return selectedProfile;
	}
}
