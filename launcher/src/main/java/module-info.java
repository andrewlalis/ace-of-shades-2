module aos2_launcher {
	requires javafx.base;
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.fxml;

	exports nl.andrewl.aos2_launcher to javafx.graphics;
	opens nl.andrewl.aos2_launcher to javafx.fxml;
}