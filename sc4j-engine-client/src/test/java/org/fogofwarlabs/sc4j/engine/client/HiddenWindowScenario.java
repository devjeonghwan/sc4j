package org.fogofwarlabs.sc4j.engine.client;

import org.fogofwarlabs.sc4j.engine.client.host.ClientHost;
import org.fogofwarlabs.sc4j.engine.client.host.HostWindow;
import org.fogofwarlabs.sc4j.engine.client.host.WindowSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Runs an OpenGL check of the engine client defaults on a hidden host window and reads drawn pixels back.
 */
public final class HiddenWindowScenario {
    /**
     * The window size of the checks.
     */
    public static final int SIZE = 32;

    private static final @NotNull String RENDER_SMOKE_PROPERTY = "sc4j.render.smoke";
    private static final @NotNull String RENDER_SMOKE_MESSAGE = "Pass -Psc4j.render.smoke=true to run the OpenGL check.";
    private static final @NotNull String TITLE = "engine render test";
    private static final int RGBA_BYTES = 4;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    /**
     * Draws and checks while the hidden window's context is current.
     */
    public interface Check {
        /**
         * Runs the check.
         */
        void run();
    }

    /**
     * Runs a check on a hidden {@link #SIZE} by {@link #SIZE} window cleared to black, then closes the host.
     *
     * @param check the check
     */
    public static void run(@NotNull Check check) {
        run(SIZE, SIZE, check);
    }

    /**
     * Runs a check on a hidden window of a size cleared to black, then closes the host.
     *
     * @param width  the window width
     * @param height the window height
     * @param check  the check
     */
    public static void run(int width, int height, @NotNull Check check) {
        Assumptions.assumeTrue(Boolean.getBoolean(RENDER_SMOKE_PROPERTY), RENDER_SMOKE_MESSAGE);

        try (ClientHost host = ClientHost.initialize()) {
            HostWindow window = host.createWindow(WindowSettings.builder(TITLE, width, height).setDecorated(false).build());

            window.makeContextCurrent();
            host.createCapabilities();
            GL11C.glViewport(0, 0, width, height);
            GL11C.glClearColor(0, 0, 0, 1);
            GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);

            check.run();
        }
    }

    /**
     * Reads one pixel of a {@link #SIZE} by {@link #SIZE} window back.
     *
     * @param x the column, from the left
     * @param y the row, from the top
     * @return the colour as {@code 0xRRGGBB}
     */
    public static int readColor(int x, int y) {
        return readColor(x, y, SIZE);
    }

    /**
     * Reads one window pixel back.
     *
     * @param x      the column, from the left
     * @param y      the row, from the top
     * @param height the window height
     * @return the colour as {@code 0xRRGGBB}
     */
    public static int readColor(int x, int y, int height) {
        ByteBuffer pixel = MemoryUtil.memAlloc(RGBA_BYTES);

        try {
            GL11C.glFinish();
            GL11C.glReadPixels(x, height - 1 - y, 1, 1, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixel);

            return Byte.toUnsignedInt(pixel.get(0)) << RED_SHIFT | Byte.toUnsignedInt(pixel.get(1)) << GREEN_SHIFT | Byte.toUnsignedInt(pixel.get(2));
        } finally {
            MemoryUtil.memFree(pixel);
        }
    }

    private HiddenWindowScenario() {
    }
}
