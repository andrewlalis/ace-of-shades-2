package nl.andrewl.aos2_client.control;

/**
 * Represents a particular context in which player input is obtained. Different
 * contexts do different things with the input. The main game, for example, will
 * move the player when WASD keys are pressed, and a chatting context might type
 * out the keys the player presses into a chat buffer. Contexts may choose to
 * implement only some specified methods here.
 */
public interface InputContext {
	default void onEnable() {}
	default void onDisable() {}

	default void keyPress(long window, int key, int mods) {}
	default void keyRelease(long window, int key, int mods) {}
	default void keyRepeat(long window, int key, int mods) {}
	default void charInput(long window, int codePoint) {}

	default void mouseButtonPress(long window, int button, int mods) {}
	default void mouseButtonRelease(long window, int button, int mods) {}
	default void mouseScroll(long window, double xOffset, double yOffset) {}
	default void mouseCursorPos(long window, double xPos, double yPos) {}
}
