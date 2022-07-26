package nl.andrewl.aos2_server.cli;

import nl.andrewl.aos2_server.Server;
import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

@CommandLine.Command(
		name = "",
		description = "Interactive shell for server commands.",
		footer = {"", "Press Ctrl-D to exit."},
		subcommands = {StopCommand.class, PlayersCommand.class}
)
public class ServerCli implements Runnable {
	final Server server;
	PrintWriter out;

	public ServerCli(Server server) {
		this.server = server;
	}

	public void setReader(LineReader reader) {
		this.out = reader.getTerminal().writer();
	}

	@Override
	public void run() {
		out.println(new CommandLine(this).getUsageMessage());
	}

	public static void start(Server server) {
		AnsiConsole.systemInstall();
		Builtins builtins = new Builtins(Path.of("."), null, null);
		builtins.rename(Builtins.Command.TTOP, "top");
		builtins.alias("zle", "widget");
		builtins.alias("bindkey", "keymap");

		ServerCli baseCommand = new ServerCli(server);
		PicocliCommands.PicocliCommandsFactory commandsFactory = new PicocliCommands.PicocliCommandsFactory();
		CommandLine cmd = new CommandLine(baseCommand, commandsFactory);
		PicocliCommands picocliCommands = new PicocliCommands(cmd);
		picocliCommands.commandDescription("bleh");
		Parser parser = new DefaultParser();
		try (Terminal terminal = TerminalBuilder.builder().build()) {
			SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, () -> Path.of("."), null);
			systemRegistry.setCommandRegistries(builtins, picocliCommands);
			systemRegistry.register("help", picocliCommands);

			LineReader reader = LineReaderBuilder.builder()
					.terminal(terminal)
					.completer(systemRegistry.completer())
					.parser(parser)
					.variable(LineReader.LIST_MAX, 50)
					.build();
			builtins.setLineReader(reader);
			baseCommand.setReader(reader);
			commandsFactory.setTerminal(terminal);
			TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
			widgets.enable();
			KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
			keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

			String prompt = "server> ";
			String rightPrompt = "HI";

			String line;
			while (server.isRunning()) {
				try {
					systemRegistry.cleanUp();
					line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
					systemRegistry.execute(line);
				} catch (UserInterruptException e) {
					// Ignore
				} catch (EndOfFileException e) {
					break;
				} catch (Exception e) {
					systemRegistry.trace(e);
				}
			}
			// If the user exits the CLI without calling "stop", we will shut down the server automatically.
			if (server.isRunning()) {
				server.shutdown();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		AnsiConsole.systemUninstall();
	}
}
