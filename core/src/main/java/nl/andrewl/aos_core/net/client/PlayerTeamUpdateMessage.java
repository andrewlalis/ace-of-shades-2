package nl.andrewl.aos_core.net.client;

import nl.andrewl.record_net.Message;

/**
 * A message that's sent by the server to announce that a player has changed to
 * a specified team. Both the player and team should already be recognized by
 * all clients; otherwise they can ignore this.
 */
public record PlayerTeamUpdateMessage(
		int playerId,
		int teamId
) implements Message {}
