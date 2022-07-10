package nl.andrewl.aos2_server;

import nl.andrewl.aos_core.model.Player;
import nl.andrewl.aos_core.model.World;
import nl.andrewl.aos_core.net.udp.ClientInputState;
import org.joml.Math;
import org.joml.Vector2i;
import org.joml.Vector3f;
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
		updated = false; // Reset the updated flag. This will be set to true if the player was updated in this tick.

		if (isGrounded(world)) {
			tickHorizontalVelocity();
			if (lastInputState.jumping()) {
				velocity.y = JUMP_SPEED;
				updated = true;
			}
		} else {
			velocity.y -= GRAVITY * dt;
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
		final Vector3f nextTickPosition = new Vector3f(position).add(movement);
//		System.out.printf("Pos:\t\t%.3f, %.3f, %.3f%nmov:\t\t%.3f, %.3f, %.3f%nNexttick:\t%.3f, %.3f, %.3f%n",
//				position.x, position.y, position.z,
//				movement.x, movement.y, movement.z,
//				nextTickPosition.x, nextTickPosition.y, nextTickPosition.z
//		);
		checkWallCollision(world, nextTickPosition, movement);
		checkCeilingCollision(world, nextTickPosition, movement);
		checkFloorCollision(world, nextTickPosition, movement);
	}

	private void checkFloorCollision(World world, Vector3f nextTickPosition, Vector3f movement) {
		// If the player is moving up or not falling out of their current y level, no point in checking.
		if (velocity.y >= 0 || Math.floor(position.y) == Math.floor(nextTickPosition.y)) return;
		float dropHeight = Math.abs(movement.y);
		int steps = (int) Math.ceil(dropHeight);
//		System.out.printf("  dropHeight=%.3f, steps=%d%n", dropHeight, steps);
		// Get a vector describing how much we move for each 1 unit Y decreases.
		Vector3f stepSize = new Vector3f(movement).div(dropHeight);
		Vector3f potentialPosition = new Vector3f(position);
		for (int i = 0; i < steps; i++) {
			potentialPosition.add(stepSize);
//			System.out.printf("  Checking: %.3f, %.3f, %.3f%n", potentialPosition.x, potentialPosition.y, potentialPosition.z);
			if (getHorizontalSpaceOccupied(potentialPosition).stream()
					.anyMatch(p -> world.getBlockAt(p.x, potentialPosition.y, p.y) != 0)) {
//				System.out.println("    Occupied!");
				position.y = Math.ceil(potentialPosition.y);
				velocity.y = 0;
				movement.y = 0;
				updated = true;
				return; // Exit before doing any extra work.
			}
		}
	}

	private void checkCeilingCollision(World world, Vector3f nextTickPosition, Vector3f movement) {
		// If the player is moving down, or not moving out of their current y level, no point in checking.
		if (velocity.y <= 0 || Math.floor(position.y) == Math.floor(nextTickPosition.y)) return;
		float riseHeight = Math.abs(movement.y);
		int steps = (int) Math.ceil(riseHeight);
		Vector3f stepSize = new Vector3f(movement).div(riseHeight);
		Vector3f potentialPosition = new Vector3f(position);
		float playerHeight = lastInputState.crouching() ? HEIGHT_CROUCH : HEIGHT;
		for (int i = 0; i < steps; i++) {
			potentialPosition.add(stepSize);
			if (getHorizontalSpaceOccupied(potentialPosition).stream()
					.anyMatch(p -> world.getBlockAt(p.x, potentialPosition.y + playerHeight, p.y) != 0)) {
				position.y = Math.floor(potentialPosition.y);
				velocity.y = 0;
				movement.y = 0;
				updated = true;
				return; // Exit before doing any extra work.
			}
		}
	}

	private void checkWallCollision(World world, Vector3f nextTickPosition, Vector3f movement) {
		// If the player isn't moving horizontally, no point in checking.
		if (velocity.x == 0 && velocity.z == 0) return;
		Vector3f potentialPosition = new Vector3f(position);
		Vector3f stepSize = new Vector3f(movement).normalize(); // Step by 1 meter each time. This will guarantee we check everything, no matter what.
		int steps = (int) Math.ceil(movement.length());
		for (int i = 0; i < steps; i++) {
			potentialPosition.add(stepSize);
			float x = potentialPosition.x;
			float y = potentialPosition.y + 1f;
			float z = potentialPosition.z;

			float borderMinZ = z - RADIUS;
			float borderMaxZ = z + RADIUS;
			float borderMinX = x - RADIUS;
			float borderMaxX = x + RADIUS;

			// -Z
			if (world.getBlockAt(x, y, borderMinZ) != 0) {
				System.out.println("-z");
				position.z = Math.ceil(borderMinZ) + RADIUS;
				velocity.z = 0;
				movement.z = 0;
				updated = true;
			}
			// +Z
			if (world.getBlockAt(x, y, borderMaxZ) != 0) {
				System.out.println("+z");
				position.z = Math.floor(borderMaxZ) - RADIUS;
				velocity.z = 0;
				movement.z = 0;
				updated = true;
			}
			// -X
			if (world.getBlockAt(borderMinX, y, z) != 0) {
				System.out.println("-x");
				position.x = Math.ceil(borderMinX) + RADIUS;
				velocity.x = 0;
				movement.z = 0;
				updated = true;
			}
			// +X
			if (world.getBlockAt(borderMaxX, y, z) != 0) {
				System.out.println("+x");
				position.x = Math.floor(borderMaxX) - RADIUS;
				velocity.x = 0;
				movement.x = 0;
				updated = true;
			}
		}
	}
}
