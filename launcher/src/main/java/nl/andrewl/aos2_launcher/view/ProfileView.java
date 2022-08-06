package nl.andrewl.aos2_launcher.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nl.andrewl.aos2_launcher.model.Profile;

public class ProfileView extends VBox {
	private final Profile profile;

	public ProfileView(Profile profile) {
		this.profile = profile;
		var nameLabel = new Label();
		nameLabel.textProperty().bind(profile.nameProperty());
		var descriptionLabel = new Label();
		descriptionLabel.textProperty().bind(profile.descriptionProperty());
		var versionLabel = new Label();
		versionLabel.textProperty().bind(profile.clientVersionProperty());
		getChildren().addAll(nameLabel, descriptionLabel, versionLabel);
		getStyleClass().add("list-item");
	}

	public Profile getProfile() {
		return this.profile;
	}
}
