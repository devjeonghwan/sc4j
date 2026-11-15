package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Runs a client on the windowing library: it initializes the library, creates the windows and their OpenGL contexts, runs
 * the frame loop of a {@link HostApplication} with frame pacing ({@link FramePacer}) and releases everything in reverse
 * order. One host exists per process; it is used from the thread that created it. The engine default runs on GLFW
 * ({@link #initialize}).
 * <p>
 * The frame loop runs until the main window is asked to close or a frame returns {@link FrameResult#LAST}. Each frame:
 * {@link HostApplication#beginFrame}, event polling, {@link HostApplication#runFrame}, then for a presented frame the wait
 * under {@link HostApplication#getFrameRateLimit}.
 */
public interface ClientHost extends AutoCloseable {
    /**
     * Creates a {@link ClientHost} instance of the engine default windowing library (GLFW): installs an error callback that prints
     * the library's errors to the standard error stream and initializes the library.
     *
     * @return the created {@link ClientHost} instance
     * @throws IllegalStateException if the windowing library cannot be initialized
     */
    static @NotNull ClientHost initialize() {
        return GlfwClientHost.initialize();
    }

    /**
     * @return the seconds since the windowing library was initialized
     */
    double getTime();


    /**
     * Creates a hidden window with an OpenGL core profile context of the settings' version. Its context is not made current.
     *
     * @param settings the window settings
     * @return the created {@link HostWindow} instance, closed by the host
     * @throws IllegalStateException if the window cannot be created
     */
    @NotNull HostWindow createWindow(@NotNull WindowSettings settings);

    /**
     * Creates a hidden window of the given size whose context shares the objects (textures, buffers) of another window's
     * context, for a loader thread that uploads while the frame loop draws. It has the other window's settings otherwise.
     *
     * @param window the window of this host whose context objects are shared
     * @param width  the width in screen coordinates
     * @param height the height in screen coordinates
     * @return the created {@link HostWindow} instance, closed by the host, or {@code null} if it cannot be created
     * @throws IllegalArgumentException if the window is not a window of this host
     */
    @Nullable HostWindow createSharedContextWindow(@NotNull HostWindow window, int width, int height);

    /**
     * Creates the OpenGL function table of the context current on this thread; call it once after
     * {@link HostWindow#makeContextCurrent}.
     */
    void createCapabilities();

    /**
     * Polls the pending window events; their callbacks run on this thread.
     */
    void pollEvents();

    /**
     * Waits for a window event or the timeout, then handles the pending events, for example while a window is minimized.
     *
     * @param timeoutSeconds the longest wait in seconds
     */
    void waitEvents(double timeoutSeconds);

    /**
     * Runs the frame loop of an application on a window until the window is asked to close or a frame is the last.
     *
     * @param window      the main window
     * @param application the application
     * @throws IOException if a frame cannot read or write a file it needs
     */
    void run(@NotNull HostWindow window, @NotNull HostApplication application) throws IOException;


    /**
     * Disposes resources: destroys the windows in reverse creation order, clears the thread's OpenGL function table,
     * terminates the windowing library and frees its error callback. A failing step is logged and the later steps still run.
     * Closing again does nothing.
     */
    @Override
    void close();
}
