package nl.andrewl.aos2_launcher.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class Profile {
	private final UUID id;
	private final StringProperty name;
	private final StringProperty description;
	private final StringProperty clientVersion;

	public Profile() {
		this(UUID.randomUUID(), "", null, null);
	}

	public Profile(UUID id, String name, String description, String clientVersion) {
		this.id = id;
		this.name = new SimpleStringProperty(name);
		this.description = new SimpleStringProperty(description);
		this.clientVersion = new SimpleStringProperty(clientVersion);
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

	public String getDescription() {
		return description.get();
	}

	public StringProperty descriptionProperty() {
		return description;
	}

	public String getClientVersion() {
		return clientVersion.get();
	}

	public StringProperty clientVersionProperty() {
		return clientVersion;
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public void setDescription(String description) {
		this.description.set(description);
	}

	public void setClientVersion(String clientVersion) {
		this.clientVersion.set(clientVersion);
	}
}
