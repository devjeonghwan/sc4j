package org.fogofwarlabs.sc4j.engine.client.render;

import org.jetbrains.annotations.NotNull;

/**
 * Holds a linked GPU shader program of a {@link GraphicsDevice}: a vertex and a fragment stage that a renderer draws its own passes with.
 * The renderer sets the program's uniforms and issues its draws on the device's context while the program is in use.
 */
public interface ShaderProgram extends AutoCloseable {
    /**
     * Makes this program the one the following draws of the device's context use.
     *
     * @throws IllegalStateException if the program is closed
     */
    void use();

    /**
     * Returns the location of a uniform of this program.
     *
     * @param name the uniform name as the shader sources declare it
     * @return the location, or {@code -1} if the program has no active uniform of the name
     * @throws IllegalStateException if the program is closed
     */
    int getUniformLocation(@NotNull String name);


    /**
     * Disposes resources: deletes the program. Closing again does nothing.
     */
    @Override
    void close();
}
