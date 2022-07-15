package nl.andrewl.aos2_launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The main starting point for the launcher app.
 */
public class Launcher extends Application {
	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader loader = new FXMLLoader(Launcher.class.getResource("/main_view.fxml"));
		Parent rootNode = loader.load();
		Scene scene = new Scene(rootNode);
		scene.getStylesheets().add(Launcher.class.getResource("/styles.css").toExternalForm());
		stage.setScene(scene);
		stage.setTitle("Ace of Shades 2 - Launcher");
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
