package nl.andrewl.aos2_client.config;

public class ClientConfig {
	public String serverHost = "localhost";
	public int serverPort = 25565;
	public String username = "player";
	public InputConfig input = new InputConfig();
	public DisplayConfig display = new DisplayConfig();

	public static class InputConfig {
		public float mouseSensitivity = 0.005f;
	}

	public static class DisplayConfig {
		public boolean fullscreen = false;
		public boolean captureCursor = true;
		public float fov = 70;
	}
}