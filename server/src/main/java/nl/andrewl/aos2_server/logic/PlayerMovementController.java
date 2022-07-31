package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.world.World;

public interface PlayerMovementController {
	boolean tickMovement(float dt, ServerPlayer player, PlayerInputTracker input, Server server, World world, ServerConfig.PhysicsConfig config);
}
