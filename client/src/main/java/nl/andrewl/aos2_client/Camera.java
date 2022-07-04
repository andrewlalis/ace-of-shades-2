package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.MathUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
	private final Vector3f position;
	private final Vector2f orientation;
	private final Matrix4f viewTransform;
	private final float[] viewTransformData = new float[16];

	public Camera() {
		this.position = new Vector3f();
		this.orientation = new Vector2f();
		this.viewTransform = new Matrix4f();
	}

	public float[] getViewTransformData() {
		return viewTransformData;
	}

	public void setPosition(float x, float y, float z) {
		position.set(x, y, z);
		updateViewTransform();
	}

	public void setOrientation(float x, float y) {
		orientation.set(MathUtils.normalize(x, 0, Math.PI * 2), MathUtils.normalize(y, 0, Math.PI * 2));
		updateViewTransform();
	}

	private void updateViewTransform() {
		viewTransform.identity();
		viewTransform.rotate(-orientation.x, new Vector3f(1, 0, 0));
		viewTransform.rotate(-orientation.y, new Vector3f(0, 1, 0));
		viewTransform.translate(position.x, position.y, position.z);
		viewTransform.get(viewTransformData);
	}
}
