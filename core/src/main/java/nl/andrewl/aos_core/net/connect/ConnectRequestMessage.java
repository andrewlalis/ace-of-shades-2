package nl.andrewl.aos_core.net.connect;

import nl.andrewl.record_net.Message;

/**
 * The first message that a client sends via TCP to the server, to indicate
 * that they'd like to join.
 * @param username The player's chosen username.
 * @param spectator Whether the player wants to be a spectator.
 */
public record ConnectRequestMessage(String username, boolean spectator) implements Message {}
