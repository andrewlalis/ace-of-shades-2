package nl.andrewl.aos2_server.cli;

import picocli.CommandLine;

@CommandLine.Command(
		name = "stop",
		description = "Stops the server.",
		mixinStandardHelpOptions = true
)
public class StopCommand implements Runnable {
	@CommandLine.ParentCommand ServerCli cli;

	@Override
	public void run() {
		cli.server.shutdown();
	}
}
