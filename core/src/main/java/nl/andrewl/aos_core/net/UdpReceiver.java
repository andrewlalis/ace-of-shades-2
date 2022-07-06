package nl.andrewl.aos_core.net;

import nl.andrewl.aos_core.Net;
import nl.andrewl.record_net.Message;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpReceiver implements Runnable {
	public static final short MAX_PACKET_SIZE = 1400;

	private final DatagramSocket socket;
	private final UdpMessageHandler handler;

	public UdpReceiver(DatagramSocket socket, UdpMessageHandler handler) {
		this.socket = socket;
		this.handler = handler;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[MAX_PACKET_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, MAX_PACKET_SIZE);
		while (true) {
			try {
				socket.receive(packet);
				Message msg = Net.read(buffer);
				handler.handle(msg, packet);
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
