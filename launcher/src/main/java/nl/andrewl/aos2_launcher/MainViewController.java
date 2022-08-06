package nl.andrewl.aos2_launcher;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.ProfileSet;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.model.Server;
import nl.andrewl.aos2_launcher.view.BindingUtil;
import nl.andrewl.aos2_launcher.view.EditProfileDialog;
import nl.andrewl.aos2_launcher.view.ProfileView;
import nl.andrewl.aos2_launcher.view.ServerView;

import java.io.IOException;
import java.util.ArrayList;

public class MainViewController implements ProgressReporter {
	@FXML public Button playButton;
	@FXML public Button editProfileButton;
	@FXML public Button removeProfileButton;
	@FXML public VBox profilesVBox;
	@FXML public VBox serversVBox;
	@FXML public Label selectedProfileLabel;
	@FXML public Label selectedServerLabel;

	@FXML public VBox progressVBox;
	@FXML public Label progressLabel;
	@FXML public ProgressBar progressBar;

	private final ProfileSet profileSet = new ProfileSet();
	private final ObservableList<Server> servers = FXCollections.observableArrayList();
	private final ObjectProperty<Server> selectedServer = new SimpleObjectProperty<>(null);

	private final ServersFetcher serversFetcher = new ServersFetcher();

	@FXML
	public void initialize() {
		BindingUtil.mapContent(serversVBox.getChildren(), servers, ServerView::new);
		BindingUtil.mapContent(profilesVBox.getChildren(), profileSet.getProfiles(), ProfileView::new);
		profileSet.selectedProfileProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				selectedProfileLabel.setText("None");
			} else {
				selectedProfileLabel.setText(newValue.getName());
			}
		});
		selectedServer.addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				selectedServerLabel.setText("None");
			} else {
				selectedServerLabel.setText(newValue.getName());
			}
		});
		BooleanBinding playBind = profileSet.selectedProfileProperty().isNull().or(selectedServer.isNull());
		playButton.disableProperty().bind(playBind);
		editProfileButton.disableProperty().bind(profileSet.selectedProfileProperty().isNull());
		removeProfileButton.disableProperty().bind(profileSet.selectedProfileProperty().isNull());

		profilesVBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			Node target = (Node) event.getTarget();
			while (target != null) {
				if (target instanceof ProfileView view) {
					if (view.getProfile().equals(profileSet.getSelectedProfile()) && event.isControlDown()) {
						selectProfile(null);
					} else if (!event.isControlDown() && event.getClickCount() == 2) {
						selectProfile(view);
						editProfile();
					} else {
						selectProfile(view);
					}
					return;
				}
				target = target.getParent();
			}
			selectProfile(null);
		});
		serversVBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			Node target = (Node) event.getTarget();
			while (target != null) {
				if (target instanceof ServerView view) {
					if (view.getServer().equals(selectedServer.get()) && event.isControlDown()) {
						selectServer(null);
					} else {
						selectServer(view);
					}
					return;
				}
				target = target.getParent();
			}
			selectServer(null);
		});
		progressVBox.managedProperty().bind(progressVBox.visibleProperty());
		progressVBox.setVisible(false);
		profileSet.loadOrCreateStandardFile();
		updateProfileViewSelectedClass();
		refreshServers();
	}

	@FXML
	public void refreshServers() {
		serversFetcher.fetchServers()
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return new ArrayList<>();
				})
				.thenAccept(newServers -> Platform.runLater(() -> {
					this.servers.clear();
					this.servers.addAll(newServers);
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
		Profile profile = profileSet.getSelectedProfile();
		Server server = this.selectedServer.get();
		SystemVersionValidator.getJreExecutablePath(this)
						.thenAccept(jrePath -> {
							VersionFetcher.INSTANCE.ensureVersionIsDownloaded(profile.getClientVersion(), this)
									.thenAccept(clientJarPath -> {
										try {
											Process p = new ProcessBuilder()
													.command(
															jrePath.toAbsolutePath().toString(),
															"-jar",
															clientJarPath.toAbsolutePath().toString()
													)
													.directory(Launcher.BASE_DIR.toFile())
													.inheritIO()
													.start();
											p.wait();
										} catch (IOException e) {
											e.printStackTrace();
										} catch (InterruptedException e) {
											throw new RuntimeException(e);
										}
									});
						});
	}

	private void selectProfile(ProfileView view) {
		Profile profile = view == null ? null : view.getProfile();
		profileSet.selectProfile(profile);
		updateProfileViewSelectedClass();
	}

	private void updateProfileViewSelectedClass() {
		PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
		for (var node : profilesVBox.getChildren()) {
			ProfileView view = (ProfileView) node;
			view.pseudoClassStateChanged(selectedClass, view.getProfile().equals(profileSet.getSelectedProfile()));
		}
	}

	private void selectServer(ServerView view) {
		Server server = view == null ? null : view.getServer();
		selectedServer.set(server);
		updateServerViewSelectedClass();
	}

	private void updateServerViewSelectedClass() {
		PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
		for (var node : serversVBox.getChildren()) {
			ServerView view = (ServerView) node;
			view.pseudoClassStateChanged(selectedClass, view.getServer().equals(selectedServer.get()));
		}
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
