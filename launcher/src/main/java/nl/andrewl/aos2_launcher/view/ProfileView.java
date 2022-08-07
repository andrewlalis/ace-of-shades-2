package nl.andrewl.aos2_launcher.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import nl.andrewl.aos2_launcher.model.Profile;

import java.io.IOException;

public class ProfileView extends Pane {
	private final Profile profile;

	@FXML public Label nameLabel;
	@FXML public Label clientVersionLabel;
	@FXML public Label usernameLabel;

	public ProfileView(Profile profile) {
		this.profile = profile;

		try {
			FXMLLoader loader = new FXMLLoader(ProfileView.class.getResource("/profile_view.fxml"));
			loader.setController(this);
			Node node = loader.load();
			getChildren().add(node);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		nameLabel.textProperty().bind(profile.nameProperty());
		clientVersionLabel.textProperty().bind(profile.clientVersionProperty());
		usernameLabel.textProperty().bind(profile.usernameProperty());
	}

	public Profile getProfile() {
		return this.profile;
	}
}
