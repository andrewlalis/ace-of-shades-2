package nl.andrewl.aos_core;

public class MathUtils {
	public static double normalize(double value, double start, double end) {
		final double width = end - start;
		final double offsetValue = value - start;
		return offsetValue - (Math.floor(offsetValue / width) * width) + start;
	}

	public static float normalize(float value, float start, float end) {
		final float width = end - start;
		final float offsetValue = value - start;
		return offsetValue - ((float) Math.floor(offsetValue / width) * width) + start;
	}

	public static float clamp(float value, float min, float max) {
		if (value < min) return min;
		return Math.min(value, max);
	}

	public static float min(float... values) {
		float m = Float.MAX_VALUE;
		for (float v : values) {
			if (v < m) m = v;
		}
		return m;
	}
}
