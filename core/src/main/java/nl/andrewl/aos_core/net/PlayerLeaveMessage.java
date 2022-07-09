package nl.andrewl.aos_core.net;

import nl.andrewl.record_net.Message;

/**
 * Announcement that's sent when a player leaves, so that all clients can stop
 * rendering the player.
 */
public record PlayerLeaveMessage(int id) implements Message {
}
