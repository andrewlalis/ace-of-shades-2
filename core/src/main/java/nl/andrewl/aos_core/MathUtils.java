package nl.andrewl.aos_core;

public class MathUtils {
	public static double normalize(double value, double start, double end) {
		final double width = end - start;
		final double offsetValue = value - start;
		return offsetValue - (Math.floor(offsetValue / width) * width) + start;
	}
}
