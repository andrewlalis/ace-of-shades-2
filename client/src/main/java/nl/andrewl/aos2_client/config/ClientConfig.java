package nl.andrewl.aos2_client.config;

public class ClientConfig {
	public InputConfig input = new InputConfig();
	public DisplayConfig display = new DisplayConfig();

	public static class InputConfig {
		public float mouseSensitivity = 0.005f;
	}

	public static class DisplayConfig {
		public boolean fullscreen = true;
		public boolean captureCursor = true;
		public float fov = 70;
	}
}
