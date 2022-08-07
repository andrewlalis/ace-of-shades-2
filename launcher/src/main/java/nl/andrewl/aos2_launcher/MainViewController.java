package nl.andrewl.aos2_launcher;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.ProfileSet;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.model.Server;
import nl.andrewl.aos2_launcher.view.EditProfileDialog;
import nl.andrewl.aos2_launcher.view.ElementList;
import nl.andrewl.aos2_launcher.view.ProfileView;
import nl.andrewl.aos2_launcher.view.ServerView;

import java.util.ArrayList;

public class MainViewController implements ProgressReporter {
	@FXML public Button playButton;
	@FXML public Button editProfileButton;
	@FXML public Button removeProfileButton;
	@FXML public VBox profilesVBox;
	private ElementList<Profile, ProfileView> profilesList;
	@FXML public VBox serversVBox;
	private ElementList<Server, ServerView> serversList;

	@FXML public VBox progressVBox;
	@FXML public Label progressLabel;
	@FXML public ProgressBar progressBar;
	@FXML public TextField registryUrlField;

	private final ProfileSet profileSet = new ProfileSet();

	private ServersFetcher serversFetcher;

	@FXML
	public void initialize() {
		profilesList = new ElementList<>(profilesVBox, ProfileView::new, ProfileView.class, ProfileView::getProfile);
		profileSet.selectedProfileProperty().addListener((observable, oldValue, newValue) -> profileSet.save());
		// A hack since we can't bind the profilesList's elements to the profileSet's.
		profileSet.getProfiles().addListener((ListChangeListener<? super Profile>) c -> {
			var selected = profileSet.getSelectedProfile();
			profilesList.clear();
			profilesList.addAll(profileSet.getProfiles());
			profilesList.selectElement(selected);
		});
		profileSet.loadOrCreateStandardFile();
		profilesList.selectElement(profileSet.getSelectedProfile());
		profileSet.selectedProfileProperty().bind(profilesList.selectedElementProperty());

		serversList = new ElementList<>(serversVBox, ServerView::new, ServerView.class, ServerView::getServer);

		BooleanBinding playBind = profileSet.selectedProfileProperty().isNull().or(serversList.selectedElementProperty().isNull());
		playButton.disableProperty().bind(playBind);
		editProfileButton.disableProperty().bind(profileSet.selectedProfileProperty().isNull());
		removeProfileButton.disableProperty().bind(profileSet.selectedProfileProperty().isNull());

		progressVBox.managedProperty().bind(progressVBox.visibleProperty());
		progressVBox.setVisible(false);

		serversFetcher = new ServersFetcher(registryUrlField.textProperty());
		Platform.runLater(this::refreshServers);
	}

	@FXML
	public void refreshServers() {
		Window owner = this.profilesVBox.getScene().getWindow();
		serversFetcher.fetchServers(owner)
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					Platform.runLater(() -> {
						Alert alert = new Alert(Alert.AlertType.ERROR);
						alert.setHeaderText("Couldn't fetch servers.");
						alert.setContentText("An error occurred, and the list of servers couldn't be fetched: " + throwable.getMessage() + ". Are you sure that you have the correct registry URL? Check the \"Servers\" tab.");
						alert.initOwner(owner);
						alert.show();
					});
					return new ArrayList<>();
				})
				.thenAccept(newServers -> Platform.runLater(() -> {
					serversList.clear();
					serversList.addAll(newServers);
				}));
	}

	@FXML
	public void addProfile() {
		EditProfileDialog dialog = new EditProfileDialog(profilesVBox.getScene().getWindow());
		dialog.showAndWait().ifPresent(profileSet::addNewProfile);
	}

	@FXML
	public void editProfile() {
		EditProfileDialog dialog = new EditProfileDialog(profilesVBox.getScene().getWindow(), profileSet.getSelectedProfile());
		dialog.showAndWait();
		profileSet.save();
	}

	@FXML
	public void removeProfile() {
		profileSet.removeSelectedProfile();
	}

	@FXML
	public void play() {
		new GameRunner().run(
				profileSet.getSelectedProfile(),
				serversList.getSelectedElement(),
				this,
				this.profilesVBox.getScene().getWindow()
		);
	}

	@Override
	public void enableProgress() {
		Platform.runLater(() -> {
			progressVBox.setVisible(true);
			progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
			progressLabel.setText(null);
		});
	}

	@Override
	public void disableProgress() {
		Platform.runLater(() -> progressVBox.setVisible(false));
	}

	@Override
	public void setActionText(String text) {
		Platform.runLater(() -> progressLabel.setText(text));
	}

	@Override
	public void setProgress(double progress) {
		Platform.runLater(() -> progressBar.setProgress(progress));
	}
}
