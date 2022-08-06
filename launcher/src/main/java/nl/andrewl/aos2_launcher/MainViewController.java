package nl.andrewl.aos2_launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.Server;
import nl.andrewl.aos2_launcher.view.BindingUtil;
import nl.andrewl.aos2_launcher.view.EditProfileDialog;
import nl.andrewl.aos2_launcher.view.ProfileView;
import nl.andrewl.aos2_launcher.view.ServerView;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class MainViewController {
	@FXML
	public Button playButton;
	@FXML
	public Button editProfileButton;
	@FXML
	public Button removeProfileButton;
	@FXML
	public VBox profilesVBox;
	@FXML
	public VBox serversVBox;
	@FXML
	public Label selectedProfileLabel;
	@FXML
	public Label selectedServerLabel;

	private final ObservableList<Profile> profiles = FXCollections.observableArrayList();
	private final ObservableList<Server> servers = FXCollections.observableArrayList();
	private final ObjectProperty<Server> selectedServer = new SimpleObjectProperty<>(null);
	private final ObjectProperty<Profile> selectedProfile = new SimpleObjectProperty<>(null);

	private final ServersFetcher serversFetcher = new ServersFetcher();

	@FXML
	public void initialize() {
		BindingUtil.mapContent(serversVBox.getChildren(), servers, ServerView::new);
		BindingUtil.mapContent(profilesVBox.getChildren(), profiles, ProfileView::new);
		selectedProfile.addListener((observable, oldValue, newValue) -> {
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
		BooleanBinding playBind = selectedProfile.isNull().or(selectedServer.isNull());
		playButton.disableProperty().bind(playBind);
		editProfileButton.disableProperty().bind(selectedProfile.isNull());
		removeProfileButton.disableProperty().bind(selectedProfile.isNull());

		profilesVBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			Node target = (Node) event.getTarget();
			while (target != null) {
				if (target instanceof ProfileView view) {
					if (view.getProfile().equals(selectedProfile.get()) && event.isControlDown()) {
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
		loadProfiles();
		refreshServers();
	}

	@FXML
	public void refreshServers() {
		serversFetcher.fetchServers()
				.exceptionally(throwable -> {
					throwable.printStackTrace();
					return new ArrayList<>();
				})
				.thenAccept(newServers -> {
					Platform.runLater(() -> {
						this.servers.clear();
						this.servers.addAll(newServers);
					});
				});
	}

	@FXML
	public void addProfile() {
		EditProfileDialog dialog = new EditProfileDialog(profilesVBox.getScene().getWindow());
		dialog.showAndWait().ifPresent(profiles::add);
		saveProfiles();
	}

	@FXML
	public void editProfile() {
		EditProfileDialog dialog = new EditProfileDialog(profilesVBox.getScene().getWindow(), selectedProfile.get());
		dialog.showAndWait();
		saveProfiles();
	}

	@FXML
	public void removeProfile() {
		if (selectedProfile.getValue() != null) {
			profiles.remove(selectedProfile.getValue());
			saveProfiles();
		}
	}

	@FXML
	public void play() {

	}

	private void selectProfile(ProfileView view) {
		Profile profile = view == null ? null : view.getProfile();
		selectedProfile.set(profile);
		PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
		for (var node : profilesVBox.getChildren()) {
			node.pseudoClassStateChanged(selectedClass, false);
		}
		if (view != null) {
			view.pseudoClassStateChanged(selectedClass, true);
		}
	}

	private void selectServer(ServerView view) {
		Server server = view == null ? null : view.getServer();
		selectedServer.set(server);
		PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");
		for (var node : serversVBox.getChildren()) {
			node.pseudoClassStateChanged(selectedClass, false);
		}
		if (view != null) {
			view.pseudoClassStateChanged(selectedClass, true);
		}
	}

	private void loadProfiles() {
		if (!Files.exists(Launcher.PROFILES_FILE)) return;
		try (var reader = Files.newBufferedReader(Launcher.PROFILES_FILE)) {
			profiles.clear();
			JsonArray array = new Gson().fromJson(reader, JsonArray.class);
			for (var element : array) {
				if (element.isJsonObject()) {
					JsonObject obj = element.getAsJsonObject();
					Profile profile = new Profile();
					profile.setName(obj.get("name").getAsString());
					profile.setDescription(obj.get("description").getAsString());
					profile.setClientVersion(obj.get("clientVersion").getAsString());
					profiles.add(profile);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveProfiles() {
		JsonArray array = new JsonArray(profiles.size());
		for (var profile : profiles) {
			JsonObject obj = new JsonObject();
			obj.addProperty("name", profile.getName());
			obj.addProperty("description", profile.getDescription());
			obj.addProperty("clientVersion", profile.getClientVersion());
			array.add(obj);
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try (var writer = Files.newBufferedWriter(Launcher.PROFILES_FILE)) {
			gson.toJson(array, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
