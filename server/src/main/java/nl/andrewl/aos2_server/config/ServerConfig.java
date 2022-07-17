package nl.andrewl.aos2_server.config;

public class ServerConfig {
	public int port = 25565;
	public int connectionBacklog = 5;
	public float ticksPerSecond = 20.0f;
	public PhysicsConfig physics = new PhysicsConfig();
	public ActionsConfig actions = new ActionsConfig();

	public static class PhysicsConfig {
		public float gravity = 9.81f * 3;
		public float walkingSpeed = 4;
		public float crouchingSpeed = 1.5f;
		public float sprintingSpeed = 9;
		public float movementAcceleration = 2;
		public float movementDeceleration = 1;
		public float jumpVerticalSpeed = 8;
	}

	public static class ActionsConfig {
		public float blockRemoveCooldown = 0.25f;
		public float blockPlaceCooldown = 0.1f;
	}
}
