package nl.andrewl.aos2_client.render;

import nl.andrewl.aos_core.FileUtils;

import java.io.IOException;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a shader program with one or more individual shaders.
 */
public class ShaderProgram {
	/**
	 * The id of the generated shader program.
	 */
	private final int id;

	public ShaderProgram(int id) {
		this.id = id;
	}

	/**
	 * Called to set this program as the one currently used by the OpenGL
	 * context. Call this before any other operation which uses the program,
	 * like {@link ShaderProgram#getUniform(String)} or drawing.
	 */
	public void use() {
		glUseProgram(id);
	}

	public void stopUsing() {
		glUseProgram(0);
	}

	public int getId() {
		return id;
	}

	public int getUniform(String name) {
		return glGetUniformLocation(id, name);
	}

	public void bindAttribute(int attribute, String variableName) {
		glBindAttribLocation(id, attribute, variableName);
	}

	public void free() {
		glDeleteProgram(id);
	}

	public static class Builder {
		private final int id;

		public Builder() {
			this.id = glCreateProgram();
		}

		public Builder withShader(String resource, int type) {
			int shaderId = glCreateShader(type);
			try {
				glShaderSource(shaderId, FileUtils.readClasspathFile(resource));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			glCompileShader(shaderId);
			glAttachShader(id, shaderId);
			glDeleteShader(shaderId);
			return this;
		}

		public ShaderProgram build() {
			glValidateProgram(id);
			glLinkProgram(id);
			return new ShaderProgram(id);
		}

		public void discard() {
			glDeleteProgram(id);
		}
	}
}
