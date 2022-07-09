package nl.andrewl.aos_core.model;

import org.joml.Vector3f;

import java.awt.*;

/**
 * A palette of 127 colors that can be used for coloring a world.
 */
public class ColorPalette {
	public static final int MAX_COLORS = 127;

	private final Vector3f[] colors = new Vector3f[MAX_COLORS];

	public ColorPalette() {
		for (int i = 0; i < MAX_COLORS; i++) {
			colors[i] = new Vector3f();
		}
	}

	public Vector3f getColor(byte value) {
		if (value < 0) return null;
		return colors[value - 1];
	}

	public void setColor(byte value, float r, float g, float b) {
		if (value < 0) return;
		colors[value - 1].set(r, g, b);
	}

	public static ColorPalette grayscale() {
		ColorPalette palette = new ColorPalette();
		for (int i = 0; i < MAX_COLORS; i++) {
			palette.colors[i].set((float) i / MAX_COLORS);
		}
		return palette;
	}

	public static ColorPalette rainbow() {
		ColorPalette palette = new ColorPalette();
		for (int i = 0; i < MAX_COLORS; i++) {
			Color c = Color.getHSBColor((float) i / MAX_COLORS, 0.8f, 0.8f);
			float[] values = c.getRGBColorComponents(null);
			palette.colors[i].x = values[0];
			palette.colors[i].y = values[1];
			palette.colors[i].z = values[2];
		}
		return palette;
	}
}
