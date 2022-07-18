package nl.andrewl.aos_core.net.connect;

import nl.andrewl.record_net.Message;

public record ConnectRequestMessage(String username) implements Message {}
