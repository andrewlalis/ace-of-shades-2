package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

public record ConnectRequestMessage(String username, int udpPort) implements Message {}
