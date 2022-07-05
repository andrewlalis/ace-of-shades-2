package nl.andrewl.aos_core;

import nl.andrewl.aos_core.net.ConnectRequestMessage;
import nl.andrewl.record_net.Message;
import nl.andrewl.record_net.Serializer;
import nl.andrewl.record_net.util.ExtendedDataInputStream;
import nl.andrewl.record_net.util.ExtendedDataOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Common wrapper for message serialization. All methods in this class are
 * thread-safe and meant for general use with any input or output streams.
 */
public final class Net {
	private Net() {}

	private static final Serializer serializer = new Serializer();
	static {
		serializer.registerType(1, ConnectRequestMessage.class);
	}

	public static ExtendedDataInputStream getInputStream(InputStream in) {
		return new ExtendedDataInputStream(serializer, in);
	}

	public static ExtendedDataOutputStream getOutputStream(OutputStream out) {
		return new ExtendedDataOutputStream(serializer, out);
	}

	public static void write(Message msg, ExtendedDataOutputStream out) throws IOException {
		serializer.writeMessage(msg, out);
	}

	public static byte[] write(Message msg) throws IOException {
		return serializer.writeMessage(msg);
	}

	public static Message read(ExtendedDataInputStream in) throws IOException {
		return serializer.readMessage(in);
	}

	public static Message read(byte[] data) throws IOException {
		return serializer.readMessage(data);
	}
}
