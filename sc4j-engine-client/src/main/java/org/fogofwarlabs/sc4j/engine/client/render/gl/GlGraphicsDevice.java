package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.engine.client.render.GpuCanvas;
import org.fogofwarlabs.sc4j.engine.client.render.GpuTimestamps;
import org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice;
import org.fogofwarlabs.sc4j.engine.client.render.ShaderProgram;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Creates the GPU resources of the current OpenGL 3.3 core profile context: the engine default {@link GraphicsDevice}. It holds no state of
 * its own; every operation works on the context current on the calling thread.
 * <p>
 * Every color target count has one shared draw buffer list ({@code GL_COLOR_ATTACHMENT0} through {@code GL_COLOR_ATTACHMENT0 + count - 1}),
 * so a selection allocates nothing.
 */
public final class GlGraphicsDevice implements GraphicsDevice {
    /**
     * The draw buffer lists by color target count.
     */
    private static final int @NotNull [] @NotNull [] DRAW_BUFFERS;
    private static final @NotNull GlGraphicsDevice INSTANCE = new GlGraphicsDevice();

    static {
        DRAW_BUFFERS = new int[MAXIMUM_COLOR_TARGETS + 1][];

        for (int count = 0; count <= MAXIMUM_COLOR_TARGETS; count++) {
            int[] drawBuffers = new int[count];

            for (int attachment = 0; attachment < count; attachment++) {
                drawBuffers[attachment] = GL_COLOR_ATTACHMENT0 + attachment;
            }

            DRAW_BUFFERS[count] = drawBuffers;
        }
    }

    /**
     * @return the shared device
     */
    public static @NotNull GlGraphicsDevice getInstance() {
        return INSTANCE;
    }

    private GlGraphicsDevice() {
    }

    /**
     * Creates a {@link GpuCanvas} instance on the current context.
     *
     * @param fieldPageSize the size in texels of the square distance field pages it draws glyphs from ({@link GpuCanvas#createSdfTexture})
     * @return the created {@link GpuCanvas} instance, closed by the caller
     * @throws IllegalStateException if a shader of the canvas does not compile or link
     */
    @Override
    public @NotNull GpuCanvas createCanvas(int fieldPageSize) {
        return new GlCanvas(fieldPageSize);
    }

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
    @Override
    public @NotNull ShaderProgram createShaderProgram(@NotNull String name, @NotNull String vertexSource, @NotNull String fragmentSource) {
        return new GlShaderProgram(GlShaderProgramUtils.linkProgram(name, vertexSource, fragmentSource));
    }

    /**
     * Selects color targets {@code 0} through {@code count - 1} of the bound framebuffer as the targets that the fragment outputs write, in
     * order. A selection allocates nothing.
     *
     * @param count the number of color targets, {@code 0} to {@link #MAXIMUM_COLOR_TARGETS}
     * @throws IndexOutOfBoundsException if the count is negative or above {@link #MAXIMUM_COLOR_TARGETS}
     */
    @Override
    public void selectColorTargets(int count) {
        glDrawBuffers(DRAW_BUFFERS[count]);
    }

    /**
     * Creates a {@link GpuTimestamps} instance on the current context.
     *
     * @param count the number of timestamp slots
     * @return the created {@link GpuTimestamps} instance, closed by the caller
     */
    @Override
    public @NotNull GpuTimestamps createTimestamps(int count) {
        return new GlGpuTimestamps(count);
    }

    /**
     * Submits the commands issued so far to the GPU without waiting for them.
     */
    @Override
    public void flush() {
        glFlush();
    }
}
