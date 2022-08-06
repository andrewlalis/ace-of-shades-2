module aos2_launcher {
	requires javafx.base;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;

	requires java.net.http;
	requires com.google.gson;

	exports nl.andrewl.aos2_launcher to javafx.graphics;
	opens nl.andrewl.aos2_launcher to javafx.fxml;
	opens nl.andrewl.aos2_launcher.view to javafx.fxml;
}