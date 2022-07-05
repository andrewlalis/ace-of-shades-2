package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

public record ConnectRejectMessage(String reason) implements Message {}
