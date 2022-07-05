package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

/**
 * Represents the player camera in the game world.
 */
public class Camera implements GLFWCursorPosCallbackI {
	public static final Vector3f UP = new Vector3f(0, 1, 0);
	public static final Vector3f DOWN = new Vector3f(0, -1, 0);
	public static final Vector3f RIGHT = new Vector3f(1, 0, 0);
	public static final Vector3f LEFT = new Vector3f(-1, 0, 0);
	public static final Vector3f FORWARD = new Vector3f(0, 0, -1);
	public static final Vector3f BACKWARD = new Vector3f(0, 0, 1);

	/**
	 * The x, y, and z position of the camera in the world.
	 */
	private final Vector3f position;

	/**
	 * The camera's angular orientation. X refers to the rotation about the
	 * vertical axis, while Y refers to the rotation about the horizontal axis.
	 * <p>
	 *     The Y axis orientation is limited to between 0 and PI, with 0
	 *     being looking straight down, and PI looking straight up.
	 * </p>
	 * <p>
	 *     The X axis orientation is limited to between 0 and 2 PI, with 0
	 *     being looking at the - Z axis.
	 * </p>
	 */
	private final Vector2f orientation;
	private final Matrix4f viewTransform;
	private final float[] viewTransformData = new float[16];

	private float lastMouseCursorX;
	private float lastMouseCursorY;
	private float mouseCursorSensitivity = 0.005f;

	public Camera() {
		this.position = new Vector3f();
		this.orientation = new Vector2f(0, (float) (Math.PI / 2));
		this.viewTransform = new Matrix4f();
	}

	public Matrix4f getViewTransform() {
		return viewTransform;
	}

	public float[] getViewTransformData() {
		return viewTransformData;
	}

	public Vector2f getOrientation() {
		return orientation;
	}

	public void setPosition(float x, float y, float z) {
		position.set(x, y, z);
		updateViewTransform();
		System.out.printf("Position: x=%.2f, y=%.2f, z=%.2f%n", position.x, position.y, position.z);
	}

	public void setOrientation(float x, float y) {
		orientation.set(
				MathUtils.normalize(x, 0, Math.PI * 2),
				MathUtils.clamp(y, 0, (float) (Math.PI))
		);
		updateViewTransform();
	}

	public void setOrientationDegrees(float x, float y) {
		setOrientation((float) Math.toRadians(x), (float) Math.toRadians(y));
	}

	private void updateViewTransform() {
		viewTransform.identity();
		viewTransform.rotate(-orientation.y + ((float) Math.PI / 2), RIGHT);
		viewTransform.rotate(-orientation.x, UP);
		viewTransform.translate(-position.x, -position.y, -position.z);
		viewTransform.get(viewTransformData);
	}

	@Override
	public void invoke(long windowHandle, double xPos, double yPos) {
		double[] xb = new double[1];
		double[] yb = new double[1];
		glfwGetCursorPos(windowHandle, xb, yb);
		float x = (float) xb[0];
		float y = (float) yb[0];
		float dx = x - lastMouseCursorX;
		float dy = y - lastMouseCursorY;
		lastMouseCursorX = x;
		lastMouseCursorY = y;
		setOrientation(orientation.x - dx * mouseCursorSensitivity, orientation.y - dy * mouseCursorSensitivity);
//		System.out.printf("rX=%.0f deg about the Y axis, rY=%.0f deg about the X axis%n", Math.toDegrees(orientation.x), Math.toDegrees(orientation.y));
		var vv = getViewVector();
//		System.out.printf("View vector: [%.2f, %.2f, %.2f]%n", vv.x, vv.y, vv.z);
	}

	public Vector3f getViewVector() {
		return new Vector3f(
				(float) -Math.sin(orientation.x),
				(float) -Math.cos(orientation.y),
				(float) Math.cos(orientation.x)
		);
	}

	public void move(Vector3f relativeMotion) {
		Vector3f actualMotion = new Vector3f(relativeMotion).mul(0.1f);
		Matrix4f moveTransform = new Matrix4f();
		moveTransform.rotate(orientation.x, UP);
		moveTransform.transformDirection(actualMotion);
		position.add(actualMotion);
		updateViewTransform();
		System.out.printf("Position: x=%.2f, y=%.2f, z=%.2f%n", position.x, position.y, position.z);
	}
}
