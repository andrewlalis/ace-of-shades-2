package nl.andrewl.aos2_client.control.context;

import nl.andrewl.aos2_client.control.InputContext;
import nl.andrewl.aos2_client.control.InputHandler;
import nl.andrewl.aos2_client.util.WindowUtils;
import nl.andrewl.aos_core.net.client.ChatWrittenMessage;

import static org.lwjgl.glfw.GLFW.*;

public class ChattingContext implements InputContext {
	private static final int MAX_LENGTH = 120;

	private final InputHandler inputHandler;

	private final StringBuffer chatBuffer = new StringBuffer(MAX_LENGTH);

	public ChattingContext(InputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	public void appendToChat(int codePoint) {
		appendToChat(Character.toString(codePoint));
	}

	public void appendToChat(String content) {
		if (chatBuffer.length() + content.length() > MAX_LENGTH) return;
		chatBuffer.append(content);
	}

	private void deleteFromChat() {
		if (chatBuffer.length() > 0) {
			chatBuffer.deleteCharAt(chatBuffer.length() - 1);
		}
	}

	private void clearChatBuffer() {
		chatBuffer.delete(0, chatBuffer.length());
	}

	private void sendChat() {
		String text = chatBuffer.toString().trim();
		if (!text.isBlank()) {
			inputHandler.getComm().sendMessage(new ChatWrittenMessage(text));
		}
		inputHandler.switchToNormalContext();
	}

	public String getChatBufferText() {
		return new String(chatBuffer);
	}

	@Override
	public void onEnable() {
		clearChatBuffer();
		if (inputHandler.getClient().getConfig().display.captureCursor) {
			WindowUtils.freeCursor(inputHandler.getWindowId());
		}
	}

	@Override
	public void onDisable() {
		clearChatBuffer();
		if (inputHandler.getClient().getConfig().display.captureCursor) {
			WindowUtils.captureCursor(inputHandler.getWindowId());
		}
	}

	@Override
	public void keyPress(long window, int key, int mods) {
		switch (key) {
			case GLFW_KEY_BACKSPACE -> deleteFromChat();
			case GLFW_KEY_ENTER -> sendChat();
			case GLFW_KEY_ESCAPE -> inputHandler.switchToNormalContext();
		}
	}

	@Override
	public void keyRepeat(long window, int key, int mods) {
		switch (key) {
			case GLFW_KEY_BACKSPACE -> deleteFromChat();
		}
	}

	@Override
	public void charInput(long window, int codePoint) {
		appendToChat(codePoint);
	}
}
