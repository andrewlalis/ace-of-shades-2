package nl.andrewl.aos2_client.render;

import nl.andrewl.aos2_client.Camera;
import nl.andrewl.aos2_client.render.model.Model;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL46.*;

/**
 * A renderer that handles rendering of textured models.
 */
public class ModelRenderer {
	private final ShaderProgram shaderProgram;
	private final int projectionUniform;
	private final int viewUniform;
	private final int modelUniform;
	private final int textureSamplerUniform;

	public ModelRenderer() {
		shaderProgram = new ShaderProgram.Builder()
				.withShader("shader/model/vertex.glsl", GL_VERTEX_SHADER)
				.withShader("shader/model/fragment.glsl", GL_FRAGMENT_SHADER)
				.build();
		projectionUniform = shaderProgram.getUniform("projectionTransform");
		viewUniform = shaderProgram.getUniform("viewTransform");
		modelUniform = shaderProgram.getUniform("modelTransform");
		textureSamplerUniform = shaderProgram.getUniform("textureSampler");
	}

	public void setPerspective(float[] data) {
		shaderProgram.use();
		glUniformMatrix4fv(projectionUniform, false, data);
		shaderProgram.stopUsing();
	}

	public void setView(float[] data) {
		shaderProgram.use();
		glUniformMatrix4fv(viewUniform, false, data);
		shaderProgram.stopUsing();
	}

	public void render(Model model, Matrix4f modelTransform) {
		shaderProgram.use();
		glUniformMatrix4fv(modelUniform, false, modelTransform.get(new float[16]));
		glUniform1i(textureSamplerUniform, 0);
		model.draw();
		shaderProgram.stopUsing();
	}

	public void free() {
		shaderProgram.free();
	}
}
