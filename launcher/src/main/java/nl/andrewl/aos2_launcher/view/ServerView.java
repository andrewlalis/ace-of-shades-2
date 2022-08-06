package nl.andrewl.aos2_launcher.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import nl.andrewl.aos2_launcher.model.Server;

public class ServerView extends VBox {
	private final Server server;

	public ServerView(Server server) {
		this.server = server;
		var hostLabel = new Label();
		hostLabel.textProperty().bind(server.hostProperty());
		var portLabel = new Label();
		portLabel.setText(Integer.toString(server.getPort()));
		server.portProperty().addListener((observableValue, x1, x2) -> {
			portLabel.setText(x2.toString());
		});
		var nameLabel = new Label();
		nameLabel.textProperty().bind(server.nameProperty());
		var descriptionLabel = new Label();
		descriptionLabel.textProperty().bind(server.descriptionProperty());
		var playersLabel = new Label();

		var nodes = getChildren();
		nodes.addAll(hostLabel, portLabel, nameLabel, descriptionLabel);
		getStyleClass().add("list-item");
	}

	public Server getServer() {
		return server;
	}
}
