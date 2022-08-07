package nl.andrewl.aos2_launcher.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import nl.andrewl.aos2_launcher.Launcher;

import java.nio.file.Path;
import java.util.UUID;

public class Profile {
	private final UUID id;
	private final StringProperty name;
	private final StringProperty username;
	private final StringProperty clientVersion;
	private final StringProperty jvmArgs;

	public Profile() {
		this(UUID.randomUUID(), "", "Player", null, null);
	}

	public Profile(UUID id, String name, String username, String clientVersion, String jvmArgs) {
		this.id = id;
		this.name = new SimpleStringProperty(name);
		this.username = new SimpleStringProperty(username);
		this.clientVersion = new SimpleStringProperty(clientVersion);
		this.jvmArgs = new SimpleStringProperty(jvmArgs);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public String getUsername() {
		return username.get();
	}

	public StringProperty usernameProperty() {
		return username;
	}

	public String getClientVersion() {
		return clientVersion.get();
	}

	public StringProperty clientVersionProperty() {
		return clientVersion;
	}

	public String getJvmArgs() {
		return jvmArgs.get();
	}

	public StringProperty jvmArgsProperty() {
		return jvmArgs;
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public void setUsername(String username) {
		this.username.set(username);
	}

	public void setClientVersion(String clientVersion) {
		this.clientVersion.set(clientVersion);
	}

	public void setJvmArgs(String jvmArgs) {
		this.jvmArgs.set(jvmArgs);
	}

	public Path getDir() {
		return Launcher.PROFILES_DIR.resolve(id.toString());
	}
}
