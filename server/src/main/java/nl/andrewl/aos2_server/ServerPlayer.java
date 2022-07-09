package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.net.udp.ClientInputState;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerPlayer extends Player {
	private static final Logger log = LoggerFactory.getLogger(ServerPlayer.class);

	public static final float HEIGHT = 1.8f;
	public static final float HEIGHT_CROUCH = 1.4f;
	public static final float WIDTH = 0.75f;
	public static final float RADIUS = WIDTH / 2f;

	public static final float GRAVITY = 9.81f * 3;
	public static final float SPEED_NORMAL = 4f;
	public static final float SPEED_CROUCH = 1.5f;
	public static final float SPEED_SPRINT = 9f;
	public static final float MOVEMENT_ACCELERATION = 5f;
	public static final float MOVEMENT_DECELERATION = 2f;
	public static final float JUMP_SPEED = 8f;

	private ClientInputState lastInputState;
	private boolean updated = false;

	public ServerPlayer(int id, String username) {
		super(id, username);
		// Initialize with a default state of no input.
		lastInputState = new ClientInputState(id, false, false, false, false, false, false, false);
	}

	public ClientInputState getLastInputState() {
		return lastInputState;
	}

	public void setLastInputState(ClientInputState inputState) {
		this.lastInputState = inputState;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void tick(float dt, World world) {
//		log.info("Ticking player " + id);
		updated = false; // Reset the updated flag. This will be set to true if the player was updated in this tick.

		checkBlockCollisions(dt, world);

		if (isGrounded(world)) {
//			System.out.println("g");
			tickHorizontalVelocity();
			if (lastInputState.jumping()) velocity.y = JUMP_SPEED;
		} else {
			velocity.y -= GRAVITY * dt;
			updated = true;
		}

		// Apply updated velocity to the player.
		if (velocity.lengthSquared() > 0) {
			Vector3f scaledVelocity = new Vector3f(velocity).mul(dt);
			position.add(scaledVelocity);
			updated = true;
		}

//		System.out.printf("pos: [%.3f, %.3f, %.3f]%n", position.x, position.y, position.z);
	}

	private void tickHorizontalVelocity() {
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
			acceleration.mul(MOVEMENT_ACCELERATION);
			horizontalVelocity.add(acceleration);
			final float maxSpeed;
			if (lastInputState.crouching()) {
				maxSpeed = SPEED_CROUCH;
			} else if (lastInputState.sprinting()) {
				maxSpeed = SPEED_SPRINT;
			} else {
				maxSpeed = SPEED_NORMAL;
			}
			if (horizontalVelocity.length() > maxSpeed) {
				horizontalVelocity.normalize(maxSpeed);
			}
			updated = true;
		} else if (horizontalVelocity.lengthSquared() > 0) {
			Vector3f deceleration = new Vector3f(horizontalVelocity)
					.negate().normalize()
					.mul(Math.min(horizontalVelocity.length(), MOVEMENT_DECELERATION));
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
		// Player must be flat on the top of a block.
		if (Math.floor(position.y) != position.y) return false;
		// Check to see if there's a block under any of the spaces the player is over.
		return getHorizontalSpaceOccupied().stream()
				.anyMatch(point -> world.getBlockAt(point.x, position.y - 0.1f, point.y) != 0);
	}

	private List<Vector2i> getHorizontalSpaceOccupied() {
		// Get the list of 2d x,z coordinates that we overlap with.
		List<Vector2i> points = new ArrayList<>(4); // Due to the size of radius, there can only be a max of 4 blocks.
		int minX = (int) Math.floor(position.x - RADIUS);
		int minZ = (int) Math.floor(position.z - RADIUS);
		int maxX = (int) Math.floor(position.x + RADIUS);
		int maxZ = (int) Math.floor(position.z + RADIUS);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				points.add(new Vector2i(x, z));
			}
		}
		return points;
	}

	private void checkBlockCollisions(float dt, World world) {
		final Vector3fc nextTickPosition = new Vector3f(position).add(new Vector3f(velocity).mul(dt));
		List<Vector2i> horizontalSpaces = getHorizontalSpaceOccupied();
		int minXNextTick = (int) Math.floor(nextTickPosition.x() - RADIUS);
		int minZNextTick = (int) Math.floor(nextTickPosition.z() - RADIUS);
		int maxXNextTick = (int) Math.floor(nextTickPosition.x() + RADIUS);
		int maxZNextTick = (int) Math.floor(nextTickPosition.z() + RADIUS);

		// Check if the player is about to hit a wall.
		// -Z
		if (
				world.getBlockAt(nextTickPosition.x(), nextTickPosition.y(), minZNextTick) != 0 &&
				world.getBlockAt(nextTickPosition.x(), nextTickPosition.y() + 1, minZNextTick) != 0
		) {
			System.out.println("wall -z");
			position.z = ((float) minZNextTick) + RADIUS + 0.001f;
			velocity.z = 0;
			updated = true;
		}
		// +Z
		if (
				world.getBlockAt(nextTickPosition.x(), nextTickPosition.y(), maxZNextTick) != 0 &&
						world.getBlockAt(nextTickPosition.x(), nextTickPosition.y() + 1, maxZNextTick) != 0
		) {
			System.out.println("wall +z");
			position.z = ((float) maxZNextTick) - RADIUS - 0.001f;
			velocity.z = 0;
			updated = true;
		}
		// -X
		if (
				world.getBlockAt(minXNextTick, nextTickPosition.y(), nextTickPosition.z()) != 0 &&
						world.getBlockAt(minXNextTick, nextTickPosition.y() + 1, nextTickPosition.z()) != 0
		) {
			System.out.println("wall -x");
			position.x = ((float) minXNextTick) + RADIUS + 0.001f;
			velocity.x = 0;
			updated = true;
		}
		// +X
		if (
				world.getBlockAt(maxXNextTick, nextTickPosition.y(), nextTickPosition.z()) != 0 &&
						world.getBlockAt(maxXNextTick, nextTickPosition.y() + 1, nextTickPosition.z()) != 0
		) {
			System.out.println("wall +x");
			position.x = ((float) maxXNextTick) - RADIUS - 0.001f;
			velocity.x = 0;
			updated = true;
		}

		// Check if the player is going to hit a ceiling on the next tick, and cancel velocity and set position.
		final float nextTickHeadY = nextTickPosition.y() + (lastInputState.crouching() ? HEIGHT_CROUCH : HEIGHT);
		boolean playerWillHitCeiling = horizontalSpaces.stream()
				.anyMatch(point -> world.getBlockAt(point.x, nextTickHeadY, point.y) != 0);
		if (playerWillHitCeiling) {
			position.y = Math.floor(nextTickPosition.y());
			if (velocity.y > 0) velocity.y = 0;
			updated = true;
		}

		// If the player is in the ground, or will be on the next tick, then move them up to the first valid space.
		boolean playerFootInBlock = horizontalSpaces.stream()
				.anyMatch(point -> world.getBlockAt(point.x, position.y, point.y) != 0 ||
						world.getBlockAt(point.x, nextTickPosition.y(), point.y) != 0);
		if (playerFootInBlock) {
//			System.out.println("Player foot in block.");
			int nextY = (int) Math.floor(nextTickPosition.y());
			while (true) {
//				System.out.println("Checking y = " + nextY);
				int finalNextY = nextY;
				boolean isOpen = horizontalSpaces.stream()
						.allMatch(point -> {
//							System.out.printf("[%d, %d, %d] -> %d%n", point.x, finalNextY, point.y, world.getBlockAt(point.x, finalNextY, point.y));
							return world.getBlockAt(point.x, finalNextY, point.y) == 0;
						});
				if (isOpen) {
//					System.out.println("It's clear to move player to y = " + nextY);
					// Move the player to that spot, and cancel out their velocity.
					position.y = nextY;
					velocity.y = 0;
					updated = true;
					break;
				}
				nextY++;
			}
		}
	}
}
