package nl.andrewl.aos2_server.cli;

import nl.andrewl.aos_core.model.world.WorldIO;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;

@CommandLine.Command(
		name = "save-world",
		description = "Saves the current world to a file.",
		mixinStandardHelpOptions = true
)
public class SaveWorldCommand implements Runnable {
	@CommandLine.ParentCommand ServerCli cli;

	@CommandLine.Option(names = {"-o", "--output"}, description = "The file to save to.", defaultValue = "world.wld")
	Path file;

	@Override
	public void run() {
		try {
			cli.out.println("Saving world...");
			WorldIO.write(cli.server.getWorld(), file);
			cli.out.println("Saved server's world to " + file.toAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
