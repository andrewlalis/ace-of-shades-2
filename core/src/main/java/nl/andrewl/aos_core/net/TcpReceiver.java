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

	public TcpReceiver(ExtendedDataInputStream in, Consumer<Message> messageConsumer) {
		this.in = in;
		this.messageConsumer = messageConsumer;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Message msg = Net.read(in);
				messageConsumer.accept(msg);
			} catch (SocketException e) {
				if (e.getMessage().equals("Socket closed")) {
					return;
				}
				e.printStackTrace();
			} catch (EOFException e) {
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
