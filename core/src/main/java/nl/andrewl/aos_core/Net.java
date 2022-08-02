package nl.andrewl.aos_core;

import nl.andrewl.aos_core.net.client.*;
import nl.andrewl.aos_core.net.connect.ConnectAcceptMessage;
import nl.andrewl.aos_core.net.connect.ConnectRejectMessage;
import nl.andrewl.aos_core.net.connect.ConnectRequestMessage;
import nl.andrewl.aos_core.net.connect.DatagramInit;
import nl.andrewl.aos_core.net.world.ChunkDataMessage;
import nl.andrewl.aos_core.net.world.ChunkHashMessage;
import nl.andrewl.aos_core.net.world.ChunkUpdateMessage;
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
		int i = 1;
		// Basic protocol messages.
		serializer.registerType(i++, ConnectRequestMessage.class);
		serializer.registerType(i++, ConnectAcceptMessage.class);
		serializer.registerType(i++, ConnectRejectMessage.class);
		serializer.registerType(i++, DatagramInit.class);

		// World messages.
		serializer.registerType(i++, ChunkHashMessage.class);
		serializer.registerType(i++, ChunkDataMessage.class);
		serializer.registerType(i++, ChunkUpdateMessage.class);
		serializer.registerType(i++, ProjectileMessage.class);

		// Player/client messages.
		serializer.registerType(i++, ClientInputState.class);
		serializer.registerType(i++, ClientOrientationState.class);
		serializer.registerType(i++, ClientHealthMessage.class);
		serializer.registerType(i++, PlayerUpdateMessage.class);
		serializer.registerType(i++, PlayerJoinMessage.class);
		serializer.registerType(i++, PlayerLeaveMessage.class);
		serializer.registerType(i++, PlayerTeamUpdateMessage.class);
		serializer.registerType(i++, BlockColorMessage.class);
		serializer.registerType(i++, InventorySelectedStackMessage.class);
		serializer.registerType(i++, ChatMessage.class);
		serializer.registerType(i++, ChatWrittenMessage.class);
		serializer.registerType(i++, ClientRecoilMessage.class);
		// Separate serializers for client inventory messages.
		serializer.registerTypeSerializer(i++, new InventorySerializer());
		serializer.registerTypeSerializer(i++, new ItemStackSerializer());

		serializer.registerType(i++, SoundMessage.class);
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
