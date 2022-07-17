package nl.andrewl.aos2_server.logic;

import nl.andrewl.aos2_server.Server;
import nl.andrewl.aos2_server.ServerPlayer;
import nl.andrewl.aos2_server.config.ServerConfig;
import nl.andrewl.aos_core.model.world.World;
import nl.andrewl.aos_core.net.udp.ChunkUpdateMessage;
import nl.andrewl.aos_core.net.udp.ClientInputState;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

import static nl.andrewl.aos2_server.ServerPlayer.*;

/**
 * Component that manages a server player's current actions and movement.
 */
public class PlayerActionManager {
	private final ServerPlayer player;

	private ClientInputState lastInputState;
	private long lastBlockRemovedAt = 0;
	private long lastBlockPlacedAt = 0;

	private boolean updated = false;

	public PlayerActionManager(ServerPlayer player) {
		this.player = player;
		lastInputState = new ClientInputState(player.getId(), false, false, false, false, false, false, false, false, false);
	}

	public ClientInputState getLastInputState() {
		return lastInputState;
	}

	public void setLastInputState(ClientInputState lastInputState) {
		this.lastInputState = lastInputState;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void tick(float dt, World world, Server server) {
		long now = System.currentTimeMillis();
		// Check for breaking blocks.
		if (lastInputState.hitting() && now - lastBlockRemovedAt > server.getConfig().actions.blockRemoveCooldown * 1000) {
			Vector3f eyePos = new Vector3f(player.getPosition());
			eyePos.y += getEyeHeight();
			var hit = world.getLookingAtPos(eyePos, player.getViewVector(), 10);
			if (hit != null) {
				world.setBlockAt(hit.pos().x, hit.pos().y, hit.pos().z, (byte) 0);
				lastBlockRemovedAt = now;
				server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(hit.pos(), world));
			}
		}
		// Check for placing blocks.
		if (lastInputState.interacting() && now - lastBlockPlacedAt > server.getConfig().actions.blockPlaceCooldown * 1000) {
			Vector3f eyePos = new Vector3f(player.getPosition());
			eyePos.y += getEyeHeight();
			var hit = world.getLookingAtPos(eyePos, player.getViewVector(), 10);
			if (hit != null) {
				Vector3i placePos = new Vector3i(hit.pos());
				placePos.add(hit.norm());
				world.setBlockAt(placePos.x, placePos.y, placePos.z, (byte) 1);
				lastBlockPlacedAt = now;
				server.getPlayerManager().broadcastUdpMessage(ChunkUpdateMessage.fromWorld(placePos, world));
			}
		}
		tickMovement(dt, world, server.getConfig().physics);
	}

	private void tickMovement(float dt, World world, ServerConfig.PhysicsConfig config) {
		updated = false; // Reset the updated flag. This will be set to true if the player was updated in this tick.
		var velocity = player.getVelocity();
		var position = player.getPosition();
		boolean grounded = isGrounded(world);
		tickHorizontalVelocity(config, grounded);

		if (isGrounded(world)) {
			if (lastInputState.jumping()) {
				velocity.y = config.jumpVerticalSpeed * (lastInputState.sprinting() ? 1.25f : 1f);
				updated = true;
			}
		} else {
			velocity.y -= config.gravity * dt;
			updated = true;
		}

		// Apply updated velocity to the player.
		if (velocity.lengthSquared() > 0) {
			Vector3f movement = new Vector3f(velocity).mul(dt);
			// Check for collisions if we try to move according to what the player wants.
			checkBlockCollisions(movement, world);
			position.add(movement);
			updated = true;
		}
	}

	private void tickHorizontalVelocity(ServerConfig.PhysicsConfig config, boolean doDeceleration) {
		var velocity = player.getVelocity();
		var orientation = player.getOrientation();
		Vector3f horizontalVelocity = new Vector3f(
				velocity.x == velocity.x ? velocity.x : 0f,
				0,
				velocity.z == velocity.z ? velocity.z : 0f
		);
		Vector3f acceleration = new Vector3f(0);
		if (lastInputState.forward()) acceleration.z -= 1;
		if (lastInputState.backward()) acceleration.z += 1;
		if (lastInputState.left()) acceleration.x -= 1;
		if (lastInputState.right()) acceleration.x += 1;
		if (acceleration.lengthSquared() > 0) {
			acceleration.normalize();
			acceleration.rotateAxis(orientation.x, 0, 1, 0);
			acceleration.mul(config.movementAcceleration);
			horizontalVelocity.add(acceleration);
			final float maxSpeed;
			if (lastInputState.crouching()) {
				maxSpeed = config.crouchingSpeed;
			} else if (lastInputState.sprinting()) {
				maxSpeed = config.sprintingSpeed;
			} else {
				maxSpeed = config.walkingSpeed;
			}
			if (horizontalVelocity.length() > maxSpeed) {
				horizontalVelocity.normalize(maxSpeed);
			}
			updated = true;
		} else if (doDeceleration && horizontalVelocity.lengthSquared() > 0) {
			Vector3f deceleration = new Vector3f(horizontalVelocity)
					.negate().normalize()
					.mul(Math.min(horizontalVelocity.length(), config.movementDeceleration));
			horizontalVelocity.add(deceleration);
			if (horizontalVelocity.length() < 0.1f) {
				horizontalVelocity.set(0);
			}
			updated = true;
		}

		// Update the player's velocity with what we've computed.
		velocity.x = horizontalVelocity.x;
		velocity.z = horizontalVelocity.z;
	}

	private boolean isGrounded(World world) {
		var position = player.getPosition();
		// Player must be flat on the top of a block.
		if (Math.floor(position.y) != position.y) return false;
		// Check to see if there's a block under any of the spaces the player is over.
		return getHorizontalSpaceOccupied(position).stream()
				.anyMatch(point -> world.getBlockAt(point.x, position.y - 0.1f, point.y) != 0);
	}

	/**
	 * Gets the list of all spaces occupied by a player's position, in the
	 * horizontal XZ plane. This can be between 1 and 4 spaces, depending on
	 * if the player's position is overlapping with a few blocks.
	 * @param pos The position.
	 * @return The list of 2d positions occupied.
	 */
	private List<Vector2i> getHorizontalSpaceOccupied(Vector3f pos) {
		// Get the list of 2d x,z coordinates that we overlap with.
		List<Vector2i> points = new ArrayList<>(4); // Due to the size of radius, there can only be a max of 4 blocks.
		int minX = (int) Math.floor(pos.x - RADIUS);
		int minZ = (int) Math.floor(pos.z - RADIUS);
		int maxX = (int) Math.floor(pos.x + RADIUS);
		int maxZ = (int) Math.floor(pos.z + RADIUS);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				points.add(new Vector2i(x, z));
			}
		}
		return points;
	}

	private void checkBlockCollisions(Vector3f movement, World world) {
		var position = player.getPosition();
		var velocity = player.getVelocity();
		final Vector3f nextTickPosition = new Vector3f(position).add(movement);
//		System.out.printf("Pos:\t\t%.3f, %.3f, %.3f%nmov:\t\t%.3f, %.3f, %.3f%nNexttick:\t%.3f, %.3f, %.3f%n",
//				position.x, position.y, position.z,
//				movement.x, movement.y, movement.z,
//				nextTickPosition.x, nextTickPosition.y, nextTickPosition.z
//		);
		float height = getCurrentHeight();
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

						updated = true;
					}
				}
			}
		}
	}

	public float getCurrentHeight() {
		return lastInputState.crouching() ? HEIGHT_CROUCH : HEIGHT;
	}

	public float getEyeHeight() {
		return lastInputState.crouching() ? EYE_HEIGHT_CROUCH : EYE_HEIGHT;
	}
}
