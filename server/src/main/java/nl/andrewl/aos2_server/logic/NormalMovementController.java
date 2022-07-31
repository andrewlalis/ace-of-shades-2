package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos2_server.model.ServerPlayer;
import nl.andrewl.aos_core.model.item.GunItemStack;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.net.client.ClientHealthMessage;
import nl.andrewl.aos_core.net.client.SoundMessage;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

import static nl.andrewl.aos_core.model.Player.RADIUS;

public class NormalMovementController implements PlayerMovementController {
	@Override
	public boolean tickMovement(float dt, ServerPlayer player, PlayerInputTracker input, Server server, World world, ServerConfig.PhysicsConfig config) {
		boolean updated;
		var velocity = player.getVelocity();
		var position = player.getPosition();
		boolean grounded = player.isGrounded(world);
		updated = tickHorizontalVelocity(player, input, config, grounded);

		if (grounded) {
			if (input.jumping()) {
				velocity.y = config.jumpVerticalSpeed * (input.sprinting() ? 1.25f : 1f);
				updated = true;
			}
		} else {
			velocity.y -= config.gravity * dt * 2; // Apply double-gravity to players to make the game feel faster.
			updated = true;
		}

		// Apply updated velocity to the player.
		if (velocity.lengthSquared() > 0) {
			Vector3f movement = new Vector3f(velocity).mul(dt);
			// Check for collisions if we try to move according to what the player wants.
			checkBlockCollisions(player, movement, server, world);
			position.add(movement);
			updated = true;
		}

		// Finally, check to see if the player is outside the world, and kill them if so.
		if (
				player.getPosition().x < world.getMinX() - 5 || player.getPosition().x > world.getMaxX() + 6 ||
				player.getPosition().z < world.getMinZ() - 5 || player.getPosition().z > world.getMaxZ() + 6 ||
				player.getPosition().y < world.getMinY() - 50 || player.getPosition().y > world.getMaxY() + 500
		) {
			server.getPlayerManager().playerKilled(player, null);
		}

		return updated;
	}

	private boolean tickHorizontalVelocity(ServerPlayer player, PlayerInputTracker input, ServerConfig.PhysicsConfig config, boolean grounded) {
		boolean updated = false;
		var velocity = player.getVelocity();
		var orientation = player.getOrientation();
		Vector3f horizontalVelocity = new Vector3f(
				velocity.x == velocity.x ? velocity.x : 0f,
				0,
				velocity.z == velocity.z ? velocity.z : 0f
		);
		Vector3f acceleration = new Vector3f(0);
		if (input.forward()) acceleration.z -= 1;
		if (input.backward()) acceleration.z += 1;
		if (input.left()) acceleration.x -= 1;
		if (input.right()) acceleration.x += 1;
		if (acceleration.lengthSquared() > 0) {
			acceleration.normalize();
			acceleration.rotateAxis(orientation.x, 0, 1, 0);
			float accelerationMagnitude = config.movementAcceleration;
			if (!grounded) accelerationMagnitude *= 0.25f;
			acceleration.mul(accelerationMagnitude);
			horizontalVelocity.add(acceleration);
			float maxSpeed;
			if (input.crouching()) {
				maxSpeed = config.crouchingSpeed;
			} else if (input.sprinting()) {
				maxSpeed = config.sprintingSpeed;
			} else {
				maxSpeed = config.walkingSpeed;
			}
			// If scoping, then force limit to crouching speed.
			if (input.interacting() && player.getInventory().getSelectedItemStack() instanceof GunItemStack) {
				maxSpeed = config.crouchingSpeed;
			}
			if (horizontalVelocity.length() > maxSpeed) {
				horizontalVelocity.normalize(maxSpeed);
			}
			updated = true;
		} else if (horizontalVelocity.lengthSquared() > 0) {
			float baseDecel = config.movementDeceleration;
			if (!grounded) baseDecel *= 0.25f;
			float decelerationMagnitude = Math.min(horizontalVelocity.length(), baseDecel);
			Vector3f deceleration = new Vector3f(horizontalVelocity).negate().normalize().mul(decelerationMagnitude);
			horizontalVelocity.add(deceleration);
			if (horizontalVelocity.length() < 0.1f) {
				horizontalVelocity.set(0);
			}
			updated = true;
		}

		// Update the player's velocity with what we've computed.
		velocity.x = horizontalVelocity.x;
		velocity.z = horizontalVelocity.z;
		return updated;
	}

	private void checkBlockCollisions(ServerPlayer player, Vector3f movement, Server server, World world) {
		var position = player.getPosition();
		var velocity = player.getVelocity();
		final Vector3f nextTickPosition = new Vector3f(position).add(movement);
		float height = player.getCurrentHeight();
		float delta = 0.00001f;
		final Vector3f stepSize = new Vector3f(movement).normalize(1.0f);
		// The number of steps we'll make towards the next tick position.
		int stepCount = (int) Math.ceil(movement.length());
		if (stepCount == 0) return; // No movement, so exit.
		final Vector3f nextPos = new Vector3f(position);
		final Vector3f lastPos = new Vector3f(position);
		for (int i = 0; i < stepCount; i++) {
			lastPos.set(nextPos);
			nextPos.add(stepSize);
			// If we shot past the next tick position, clamp it to that.
			if (new Vector3f(nextPos).sub(position).length() > movement.length()) {
				nextPos.set(nextTickPosition);
			}

			// Check if we collide with anything at this new position.

			float playerBodyPrevMinZ = lastPos.z - RADIUS;
			float playerBodyPrevMaxZ = lastPos.z + RADIUS;
			float playerBodyPrevMinX = lastPos.x - RADIUS;
			float playerBodyPrevMaxX = lastPos.x + RADIUS;
			float playerBodyPrevMinY = lastPos.y;
			float playerBodyPrevMaxY = lastPos.y + height;

			float playerBodyMinZ = nextPos.z - RADIUS;
			float playerBodyMaxZ = nextPos.z + RADIUS;
			float playerBodyMinX = nextPos.x - RADIUS;
			float playerBodyMaxX = nextPos.x + RADIUS;
			float playerBodyMinY = nextPos.y;
			float playerBodyMaxY = nextPos.y + height;

			// Compute the bounds of all blocks the player is intersecting with.
			int minX = (int) Math.floor(playerBodyMinX);
			int minZ = (int) Math.floor(playerBodyMinZ);
			int minY = (int) Math.floor(playerBodyMinY);
			int maxX = (int) Math.floor(playerBodyMaxX);
			int maxZ = (int) Math.floor(playerBodyMaxZ);
			int maxY = (int) Math.floor(playerBodyMaxY);

			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					for (int y = minY; y <= maxY; y++) {
						byte block = world.getBlockAt(x, y, z);
						if (block <= 0) continue; // We're not colliding with this block.
						float blockMinY = (float) y;
						float blockMaxY = (float) y + 1;
						float blockMinX = (float) x;
						float blockMaxX = (float) x + 1;
						float blockMinZ = (float) z;
						float blockMaxZ = (float) z + 1;

						/*
						To determine if the player is moving into the -Z side of a block:
						- The player's max z position went from < blockMinZ to >= blockMinZ.
						- The block to the -Z direction is air.
						 */
						boolean collidingWithNegativeZ = playerBodyPrevMaxZ < blockMinZ && playerBodyMaxZ >= blockMinZ && world.getBlockAt(x, y, z - 1) <= 0;
						if (collidingWithNegativeZ) {
							position.z = blockMinZ - RADIUS - delta;
							velocity.z = 0;
							movement.z = 0;
						}

						/*
						To determine if the player is moving into the +Z side of a block:
						- The player's min z position went from >= blockMaxZ to < blockMaxZ.
						- The block to the +Z direction is air.
						 */
						boolean collidingWithPositiveZ = playerBodyPrevMinZ >= blockMaxZ && playerBodyMinZ < blockMaxZ && world.getBlockAt(x, y, z + 1) <= 0;
						if (collidingWithPositiveZ) {
							position.z = blockMaxZ + RADIUS + delta;
							velocity.z = 0;
							movement.z = 0;
						}

						/*
						To determine if the player is moving into the -X side of a block:
						- The player's max x position went from < blockMinX to >= blockMinX
						- The block to the -X direction is air.
						 */
						boolean collidingWithNegativeX = playerBodyPrevMaxX < blockMinX && playerBodyMaxX >= blockMinX && world.getBlockAt(x - 1, y, z) <= 0;
						if (collidingWithNegativeX) {
							position.x = blockMinX - RADIUS - delta;
							velocity.x = 0;
							movement.x = 0;
						}

						/*
						To determine if the player is moving into the +X side of a block:
						- The player's min x position went from >= blockMaxX to < blockMaxX.
						- The block to the +X direction is air.
						 */
						boolean collidingWithPositiveX = playerBodyPrevMinX >= blockMaxX && playerBodyMinX < blockMaxX && world.getBlockAt(x + 1, y, z) <= 0;
						if (collidingWithPositiveX) {
							position.x = blockMaxX + RADIUS + delta;
							velocity.x = 0;
							movement.x = 0;
						}

						/*
						To determine if the player is moving down onto a block:
						- The player's min y position went from >= blockMaxY to < blockMaxY
						- The block above the current one is air.
						 */
						boolean collidingWithFloor = playerBodyPrevMinY >= blockMaxY && playerBodyMinY < blockMaxY && world.getBlockAt(x, y + 1, z) <= 0;
						if (collidingWithFloor) {
							// This is a special case! We need to check for fall damage.
							if (velocity.y < -20) {
								float damage = velocity.y / 50f;
								player.setHealth(player.getHealth() + damage);
								if (player.getHealth() <= 0) {
									server.getPlayerManager().playerKilled(player, player);
								} else {
									var handler = server.getPlayerManager().getHandler(player.getId());
									handler.sendDatagramPacket(new ClientHealthMessage(player.getHealth()));
									int soundVariant = ThreadLocalRandom.current().nextInt(1, 4);
									handler.sendDatagramPacket(new SoundMessage("hurt_" + soundVariant, 1, player.getPosition()));
								}
							}
							position.y = blockMaxY;
							velocity.y = 0;
							movement.y = 0;
						}

						/*
						To determine if the player is moving up into a block:
						- The player's y position went from below blockMinY to >= blockMinY
						- The block below the current one is air.
						 */
						boolean collidingWithCeiling = playerBodyPrevMaxY < blockMinY && playerBodyMaxY >= blockMinY && world.getBlockAt(x, y - 1, z) <= 0;
						if (collidingWithCeiling) {
							position.y = blockMinY - height - delta;
							velocity.y = 0;
							movement.y = 0;
						}
					}
				}
			}
		}
	}
}
