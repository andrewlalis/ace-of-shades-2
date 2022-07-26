package nl.andrewl.aos_core;

import java.util.regex.Pattern;

public class UsernameChecker {
	private static final int MIN_LENGTH = 3;
	private static final int MAX_LENGTH = 24;
	private static final Pattern pattern = Pattern.compile("[a-zA-Z]+[a-zA-Z\\d-_]*");

	public static boolean isValid(String username) {
		if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) return false;
		return pattern.matcher(username).matches();
	}
}
