package nl.andrewl.aos_core.net;

import nl.andrewl.aos_core.Net;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.util.ExtendedDataInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.function.Consumer;

public class TcpReceiver implements Runnable {
	private final ExtendedDataInputStream in;
	private final Consumer<Message> messageConsumer;
	private Runnable shutdownHook;

	public TcpReceiver(ExtendedDataInputStream in, Consumer<Message> messageConsumer) {
		this.in = in;
		this.messageConsumer = messageConsumer;
	}

	public TcpReceiver withShutdownHook(Runnable shutdownHook) {
		this.shutdownHook = shutdownHook;
		return this;
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			try {
				Message msg = Net.read(in);
				messageConsumer.accept(msg);
			} catch (SocketException e) {
				if (e.getMessage().equals("Socket closed") || e.getMessage().equals("Connection reset")) {
					running = false;
				} else {
					e.printStackTrace();
				}
			} catch (EOFException e) {
				running = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (shutdownHook != null) shutdownHook.run();
	}
}
