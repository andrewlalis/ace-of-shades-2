package nl.andrewl.aos2_launcher.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Profile {
	private final StringProperty name;
	private final StringProperty description;
	private final StringProperty clientVersion;

	public Profile() {
		this.name = new SimpleStringProperty("");
		this.description = new SimpleStringProperty(null);
		this.clientVersion = new SimpleStringProperty(null);
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
