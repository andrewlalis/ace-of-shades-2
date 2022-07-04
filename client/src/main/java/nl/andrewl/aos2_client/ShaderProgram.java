package nl.andrewl.aos2_client;

import nl.andrewl.aos_core.FileUtils;

import java.io.IOException;

import static org.lwjgl.opengl.GL46.*;

/**
 * Represents a shader program with one or more individual shaders.
 */
public class ShaderProgram {
	private final int id;

	public ShaderProgram(int id) {
		this.id = id;
	}

	public void use() {
		glUseProgram(id);
	}

	public int getUniform(String name) {
		return glGetUniformLocation(id, name);
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
	}
}
