package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.render.ShaderProgram;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Holds a linked OpenGL program ({@link GlShaderProgramUtils#linkProgram}): the {@link ShaderProgram} of the OpenGL default device.
 */
final class GlShaderProgram implements ShaderProgram {
    private final int program;

    private boolean closed;

    /**
     * Creates a {@link GlShaderProgram} instance over a linked program, which it deletes when it closes.
     *
     * @param program the OpenGL program name
     */
    GlShaderProgram(int program) {
        this.program = program;

        this.closed = false;
    }

    /**
     * Makes this program the one the following draws of the device's context use.
     *
     * @throws IllegalStateException if the program is closed
     */
    @Override
    public void use() {
        this.requireOpen();
        glUseProgram(this.program);
    }

    /**
     * Returns the location of a uniform of this program.
     *
     * @param name the uniform name as the shader sources declare it
     * @return the location, or {@code -1} if the program has no active uniform of the name
     * @throws IllegalStateException if the program is closed
     */
    @Override
    public int getUniformLocation(@NotNull String name) {
        this.requireOpen();

        return glGetUniformLocation(this.program, name);
    }

    private void requireOpen() {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The shader program is closed.");
        }
    }


    /**
     * Disposes resources: deletes the program. Closing again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        glDeleteProgram(this.program);
    }
}
