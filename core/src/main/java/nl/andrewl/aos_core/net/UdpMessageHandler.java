package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

import java.net.DatagramPacket;

@FunctionalInterface
public interface UdpMessageHandler {
	void handle(Message msg, DatagramPacket packet);
}
