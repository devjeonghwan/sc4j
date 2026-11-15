package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioDevice;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioMixer;
import org.fogofwarlabs.sc4j.engine.client.audio.SilentAudioDevice;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorSurface;
import org.fogofwarlabs.sc4j.engine.client.display.DisplayScale;
import org.fogofwarlabs.sc4j.engine.client.host.*;
import org.fogofwarlabs.sc4j.engine.client.render.GpuCanvas;
import org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice;
import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.fogofwarlabs.sc4j.engine.mod.ModLaunch;
import org.fogofwarlabs.sc4j.engine.mod.ModStorage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Runs an engine-only mod's client on a window: the runtime's own main loop. It opens the window with its OpenGL context, the
 * {@link GpuCanvas} of the {@link GraphicsDevice}, the audio (the OpenAL default device, or silence without a usable output), the window's
 * cursor surface and the window input, starts the mod's first scene on a {@link SceneClient} and runs the frames on the {@link ClientHost}
 * until the window is closed or a scene asks the client to end; then it closes the scenes, the cursor surface, the audio, the canvas and the
 * window in reverse order. Each frame hands the polled input to the scenes, runs the client's frame on the framebuffer and presents it with
 * vertical sync.
 * <p>
 * The client's display scale is the window's content scale (read every frame, so it follows the window to another monitor) times the
 * user's interface scale of the mod: the launch's setting at the start, then what the mod sets on the client ({@link SceneClient#setUserScale}).
 */
public final class WindowClient implements HostApplication {
    private static final @NotNull Logger LOGGER = Logger.of(WindowClient.class);
    /**
     * The page size of the canvas's distance field glyph pages.
     */
    private static final int FIELD_PAGE_SIZE = 64;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;
    /**
     * The longest wait for a window event in seconds while the window has no framebuffer (minimized).
     */
    private static final double MINIMIZED_WAIT_SECONDS = 0.1;

    /**
     * Runs the client until the window is closed or a scene asks it to end.
     *
     * @param launch    the launch request of the run
     * @param storage   the mod storage
     * @param settings  the settings of the window
     * @param userScale the user's interface scale of the mod ({@link DisplayScale#parseUserScale}), {@code 1} without the setting
     * @param factory   the mod's factory of the first scene
     * @throws ModException          if the mod refuses the launch request
     * @throws IOException           if a scene cannot read a resource it opens or draws
     * @throws IllegalStateException if the window cannot be opened
     */
    public static void run(@NotNull ModLaunch launch, @NotNull ModStorage storage, @NotNull WindowSettings settings, float userScale,
                           @NotNull FirstSceneFactory factory) throws ModException, IOException {
        try (ClientHost host = ClientHost.initialize()) {
            HostWindow window = host.createWindow(settings);

            window.makeContextCurrent();
            host.createCapabilities();
            window.setSwapInterval(HostWindow.SWAP_INTERVAL_ON);

            try (GpuCanvas canvas = GraphicsDevice.getCurrent().createCanvas(FIELD_PAGE_SIZE);
                 AudioMixer audio = new AudioMixer(openAudio(host));
                 CursorSurface cursor = window.createCursorSurface();
                 SceneClient client = new SceneClient(launch, storage, canvas, audio, cursor)) {
                WindowInput input = window.createInput(client::handleInput);
                WindowClient application = new WindowClient(host, window, input, client);

                client.setDisplayScale(DisplayScale.of(window.getContentScale(), userScale));
                client.start(factory);
                window.center();
                window.show();
                host.run(window, application);
            }
        }
    }

    private static @NotNull AudioDevice openAudio(@NotNull ClientHost host) {
        try {
            return AudioDevice.openDefault();
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.warning("SOUND DISABLED: {0}", exception.getMessage());

            return new SilentAudioDevice(() -> Math.round(host.getTime() * MILLISECONDS_PER_SECOND));
        }
    }


    private final @NotNull ClientHost host;
    private final @NotNull HostWindow window;
    private final @NotNull WindowInput input;
    private final @NotNull SceneClient client;

    private WindowClient(@NotNull ClientHost host, @NotNull HostWindow window, @NotNull WindowInput input, @NotNull SceneClient client) {
        this.host   = host;
        this.window = window;
        this.input  = input;
        this.client = client;
    }

    /**
     * Returns the frame rate limit of the frame just run. It is read after {@link #runFrame} returned a presented frame.
     *
     * @return {@code 0}: the window's vertical sync paces the frames
     */
    @Override
    public int getFrameRateLimit() {
        return 0;
    }


    /**
     * Runs one frame: input, simulation, drawing and presenting.
     * <p>
     * The window events were polled before; the client runs its frame on the framebuffer, which the window presents. While the window has
     * no framebuffer (minimized) the frame waits for window events and draws nothing.
     *
     * @return what the frame did
     * @throws IOException if the frame cannot read or write a file it needs
     */
    @Override
    public @NotNull FrameResult runFrame() throws IOException {
        int width = this.window.getFramebufferWidth();
        int height = this.window.getFramebufferHeight();

        if (width <= 0 || height <= 0) {
            this.host.waitEvents(MINIMIZED_WAIT_SECONDS);

            return this.client.isQuitRequested() ? FrameResult.LAST : FrameResult.SKIPPED;
        }

        this.input.refreshSurfaceSize();
        this.updateDisplayScale();

        FrameResult result = this.client.runFrame(Math.round(this.host.getTime() * MILLISECONDS_PER_SECOND), width, height);

        this.window.swapBuffers();

        if (result == FrameResult.LAST) {
            this.window.requestClose();
        }

        return result;
    }

    private void updateDisplayScale() {
        float contentScale = this.window.getContentScale();
        DisplayScale current = this.client.getDisplayScale();

        if (contentScale != current.getContentScale()) {
            this.client.setDisplayScale(DisplayScale.of(contentScale, current.getUserScale()));
        }
    }
}
