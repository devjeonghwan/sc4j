package org.fogofwarlabs.sc4j.engine.client.render;

import org.fogofwarlabs.sc4j.engine.client.render.gl.GlGraphicsDevice;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the GPU resources of the rendering context that is current on the calling thread: the 2D {@link GpuCanvas} of interface drawing,
 * the {@link ShaderProgram}s of a renderer's own passes, the selection of the framebuffer color targets those passes write and the
 * {@link GpuTimestamps} of frame diagnostics. The engine default is the OpenGL 3.3 core profile device ({@link #getCurrent}).
 * <p>
 * A device and the resources it creates are used from the thread whose context is current, between the context's creation and its window's
 * close.
 */
public interface GraphicsDevice {
    /**
     * The vertices of a quad that a renderer draws as two triangles.
     */
    int QUAD_VERTICES = 6;
    /**
     * The color targets that a framebuffer of every device can write at once.
     */
    int MAXIMUM_COLOR_TARGETS = 8;

    /**
     * Returns the device of the rendering context current on the calling thread: the engine default OpenGL 3.3 core profile device.
     *
     * @return the device; one shared instance that holds no state of its own
     */
    static @NotNull GraphicsDevice getCurrent() {
        return GlGraphicsDevice.getInstance();
    }

    /**
     * Creates a {@link GpuCanvas} instance on the current context.
     *
     * @param fieldPageSize the size in texels of the square distance field pages it draws glyphs from ({@link GpuCanvas#createSdfTexture})
     * @return the created {@link GpuCanvas} instance, closed by the caller
     * @throws IllegalStateException if a shader of the canvas does not compile or link
     */
    @NotNull GpuCanvas createCanvas(int fieldPageSize);

    /**
     * Creates a {@link ShaderProgram} instance: compiles a vertex and a fragment stage and links them. A failing step leaves no GPU object
     * behind.
     *
     * @param name           the renderer name that the failure messages start with
     * @param vertexSource   the vertex stage source
     * @param fragmentSource the fragment stage source
     * @return the created {@link ShaderProgram} instance, closed by the caller
     * @throws IllegalStateException if a stage does not compile or the program does not link
     */
    @NotNull ShaderProgram createShaderProgram(@NotNull String name, @NotNull String vertexSource, @NotNull String fragmentSource);

    /**
     * Selects color targets {@code 0} through {@code count - 1} of the bound framebuffer as the targets that the fragment outputs write, in
     * order. A selection allocates nothing.
     *
     * @param count the number of color targets, {@code 0} to {@link #MAXIMUM_COLOR_TARGETS}
     * @throws IndexOutOfBoundsException if the count is negative or above {@link #MAXIMUM_COLOR_TARGETS}
     */
    void selectColorTargets(int count);

    /**
     * Creates a {@link GpuTimestamps} instance on the current context.
     *
     * @param count the number of timestamp slots
     * @return the created {@link GpuTimestamps} instance, closed by the caller
     */
    @NotNull GpuTimestamps createTimestamps(int count);

    /**
     * Submits the commands issued so far to the GPU without waiting for them.
     */
    void flush();
}
