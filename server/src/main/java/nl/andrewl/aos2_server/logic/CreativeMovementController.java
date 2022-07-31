package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.world.World;
import org.joml.Vector3f;

public class CreativeMovementController implements PlayerMovementController {
	@Override
	public boolean tickMovement(float dt, ServerPlayer player, PlayerInputTracker input, Server server, World world, ServerConfig.PhysicsConfig config) {
		boolean updated = tickVelocity(player, input, config);
		if (player.getVelocity().lengthSquared() > 0) {
			Vector3f movement = new Vector3f(player.getVelocity()).mul(dt);
			player.getPosition().add(movement);
			updated = true;
		}
		return updated;
	}

	private boolean tickVelocity(ServerPlayer player, PlayerInputTracker input, ServerConfig.PhysicsConfig config) {
		boolean updated = false;
		var velocity = player.getVelocity();
		var orientation = player.getOrientation();
		Vector3f acceleration = new Vector3f(0);
		if (input.forward()) acceleration.z -= 1;
		if (input.backward()) acceleration.z += 1;
		if (input.left()) acceleration.x -= 1;
		if (input.right()) acceleration.x += 1;
		if (input.jumping()) acceleration.y += 1;
		if (input.crouching()) acceleration.y -= 1;
		if (acceleration.lengthSquared() > 0) {
			acceleration.normalize()
					.rotateY(orientation.x)
					.mul(config.movementAcceleration);
			velocity.add(acceleration);

			float maxSpeed = config.walkingSpeed;
			if (input.sprinting()) {
				maxSpeed = config.sprintingSpeed;
			}
			maxSpeed *= 4;
			if (velocity.length() > maxSpeed) {
				velocity.normalize(maxSpeed);
			}
			updated = true;
		} else if (velocity.lengthSquared() > 0) {
			float decel = Math.min(velocity.length(), config.movementDeceleration);
			Vector3f deceleration = new Vector3f(velocity).negate().normalize().mul(decel);
			velocity.add(deceleration);
			if (Math.abs(velocity.x) < 0.1f) velocity.x = 0;
			if (Math.abs(velocity.y) < 0.1f) velocity.y = 0;
			if (Math.abs(velocity.z) < 0.1f) velocity.z = 0;
			updated = true;
		}
		return updated;
	}
}
