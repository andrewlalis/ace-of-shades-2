package nl.andrewl.aos2_launcher;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.TilePane;

public class MainViewController {
	@FXML
	public TilePane profilesTilePane;


	public void onExit(ActionEvent actionEvent) {
		Platform.exit();
	}
}
