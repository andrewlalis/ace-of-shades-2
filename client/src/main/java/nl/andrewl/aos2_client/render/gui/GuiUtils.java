package nl.andrewl.aos2_client.render.gui;

import org.lwjgl.nanovg.NVGColor;

public class GuiUtils {
	public static NVGColor rgba(float r, float g, float b, float a, NVGColor color) {
		color.r(r);
		color.g(g);
		color.b(b);
		color.a(a);
		return color;
	}

	public static boolean isBlack(NVGColor color) {
		return color.r() == 0 && color.g() == 0 && color.b() == 0 && color.a() == 0;
	}
}
