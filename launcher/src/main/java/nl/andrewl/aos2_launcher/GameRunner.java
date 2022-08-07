package nl.andrewl.aos2_launcher;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.model.Server;

import java.io.IOException;
import java.nio.file.Path;

public class GameRunner {
	public void run(Profile profile, Server server, ProgressReporter progressReporter, Window owner) {
		SystemVersionValidator.getJreExecutablePath(progressReporter)
				.whenCompleteAsync((jrePath, throwable) -> {
					if (throwable != null) {
						showPopup(
								owner,
								Alert.AlertType.ERROR,
								"An error occurred while ensuring that you've got the latest Java runtime: " + throwable.getMessage()
						);
					} else {
						VersionFetcher.INSTANCE.ensureVersionIsDownloaded(profile.getClientVersion(), progressReporter)
								.whenCompleteAsync((clientJarPath, throwable2) -> {
									progressReporter.disableProgress();
									if (throwable2 != null) {
										showPopup(
												owner,
												Alert.AlertType.ERROR,
												"An error occurred while ensuring you've got the correct client version: " + throwable2.getMessage()
										);
									} else {
										startGame(owner, profile, server, jrePath, clientJarPath);
									}
								});
					}
				});
	}

	private void startGame(Window owner, Profile profile, Server server, Path jrePath, Path clientJarPath) {
		try {
			Process p = new ProcessBuilder()
					.command(
							jrePath.toAbsolutePath().toString(),
							"-jar", clientJarPath.toAbsolutePath().toString(),
							server.getHost(),
							Integer.toString(server.getPort()),
							profile.getUsername()
					)
					.directory(profile.getDir().toFile())
					.inheritIO()
					.start();
			p.wait();
		} catch (IOException e) {
			showPopup(owner, Alert.AlertType.ERROR, "An error occurred while starting the game: " + e.getMessage());
		} catch (InterruptedException e) {
			showPopup(owner, Alert.AlertType.ERROR, "The game was interrupted: " + e.getMessage());
		}
	}

	private void showPopup(Window owner, Alert.AlertType type, String text) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.initOwner(owner);
			alert.setContentText(text);
			alert.show();
		});
	}
}
