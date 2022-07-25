package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.render.model.Model;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL46.*;

/**
 * A renderer that handles rendering of textured models.
 */
public class ModelRenderer {
	private final ShaderProgram shaderProgram;
	private final int projectionUniform;
	private final int viewUniform;
	private final int modelUniform;
	private final int normalUniform;
	private final int textureSamplerUniform;
	private final int colorUniform;

	public ModelRenderer() {
		shaderProgram = new ShaderProgram.Builder()
				.withShader("shader/model/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/model/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
		System.out.println(glGetProgramInfoLog(shaderProgram.getId())); // Enable for debugging!
		projectionUniform = shaderProgram.getUniform("projectionTransform");
		viewUniform = shaderProgram.getUniform("viewTransform");
		modelUniform = shaderProgram.getUniform("modelTransform");
		normalUniform = shaderProgram.getUniform("normalTransform");
		colorUniform = shaderProgram.getUniform("aspectColor");
		textureSamplerUniform = shaderProgram.getUniform("textureSampler");
	}

	public void setPerspective(float[] data) {
		shaderProgram.use();
		glUniformMatrix4fv(projectionUniform, false, data);
		shaderProgram.stopUsing();
	}

	public void start(float[] viewTransformData) {
		shaderProgram.use();
		glUniformMatrix4fv(viewUniform, false, viewTransformData);
		glUniform1i(textureSamplerUniform, 0);
	}

	public void setAspectColor(Vector3f color) {
		glUniform3f(colorUniform, color.x, color.y, color.z);
	}

	public void render(Model model, Matrix4f modelTransform, Matrix3f normalTransform) {
		glUniformMatrix4fv(modelUniform, false, modelTransform.get(new float[16]));
		glUniformMatrix3fv(normalUniform, false, normalTransform.get(new float[16]));
		model.draw();
	}

	public void render(Model model, float[] transformData, float[] normalData) {
		glUniformMatrix4fv(modelUniform, false, transformData);
		glUniformMatrix3fv(normalUniform, false, normalData);
		model.draw();
	}

	public void end() {
		shaderProgram.stopUsing();
	}

	public void free() {
		shaderProgram.free();
	}
}
