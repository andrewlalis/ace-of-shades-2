package nl.andrewl.aos2_launcher.view;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import nl.andrewl.aos2_launcher.VersionFetcher;
import nl.andrewl.aos2_launcher.model.ClientVersionRelease;
import nl.andrewl.aos2_launcher.model.Profile;

import java.io.IOException;
import java.util.Objects;

public class EditProfileDialog extends Dialog<Profile> {
	@FXML public TextField nameField;
	@FXML public TextField usernameField;
	@FXML public ChoiceBox<String> clientVersionChoiceBox;
	@FXML public TextArea jvmArgsTextArea;

	private final ObjectProperty<Profile> profile;

	public EditProfileDialog(Window owner, Profile profile) {
		this.profile = new SimpleObjectProperty<>(profile);
		try {
			FXMLLoader loader = new FXMLLoader(EditProfileDialog.class.getResource("/dialog/edit_profile.fxml"));
			loader.setController(this);
			Parent parent = loader.load();
			initOwner(owner);
			initModality(Modality.APPLICATION_MODAL);
			setResizable(true);
			setTitle("Edit Profile");

			BooleanBinding formInvalid = nameField.textProperty().isEmpty()
					.or(clientVersionChoiceBox.valueProperty().isNull())
					.or(usernameField.textProperty().isEmpty());
			nameField.setText(profile.getName());
			usernameField.setText(profile.getUsername());
			VersionFetcher.INSTANCE.getAvailableReleases()
					.whenComplete((releases, throwable) -> Platform.runLater(() -> {
						if (throwable == null) {
							clientVersionChoiceBox.setItems(FXCollections.observableArrayList(releases.stream().map(ClientVersionRelease::tag).toList()));
							// If the profile doesn't have a set version, use the latest release.
							if (profile.getClientVersion() == null || profile.getClientVersion().isBlank()) {
								String lastRelease = releases.size() == 0 ? null : releases.get(0).tag();
								if (lastRelease != null) {
									clientVersionChoiceBox.setValue(lastRelease);
								}
							} else {
								clientVersionChoiceBox.setValue(profile.getClientVersion());
							}
						} else {
							throwable.printStackTrace();
							Alert alert = new Alert(Alert.AlertType.ERROR);
							alert.initOwner(this.getOwner());
							alert.setContentText("An error occurred while fetching the latest game releases: " + throwable.getMessage());
							alert.show();
						}
			}));
			jvmArgsTextArea.setText(profile.getJvmArgs());

			DialogPane pane = new DialogPane();
			pane.setContent(parent);
			ButtonType okButton = new ButtonType("Ok", ButtonBar.ButtonData.OK_DONE);
			ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
			pane.getButtonTypes().add(okButton);
			pane.getButtonTypes().add(cancelButton);
			pane.lookupButton(okButton).disableProperty().bind(formInvalid);
			setDialogPane(pane);
			setResultConverter(buttonType -> {
				if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
					return null;
				}
				var prof = this.profile.getValue();
				prof.setName(nameField.getText().trim());
				prof.setUsername(usernameField.getText().trim());
				prof.setClientVersion(clientVersionChoiceBox.getValue());
				prof.setJvmArgs(jvmArgsTextArea.getText());
				return this.profile.getValue();
			});
			setOnShowing(event -> Platform.runLater(() -> nameField.requestFocus()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public EditProfileDialog(Window owner) {
		this(owner, new Profile());
	}
}
