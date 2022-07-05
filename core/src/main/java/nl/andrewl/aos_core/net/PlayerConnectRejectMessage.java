package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

public record PlayerConnectRejectMessage (String reason) implements Message {}
