package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorSurface;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Shows the pointer of a {@link HostWindow} as GLFW custom cursors ({@link HostWindow#createCursorSurface}): each picture becomes one
 * platform cursor at its first showing (its anchor the hot spot, kept inside the picture), built from the detail picture of the scale, and
 * is kept until {@link #close()}; {@link #hide} shows a blank one-pixel cursor. When the platform refuses a custom cursor, the platform's
 * own arrow is shown from then on. Must be used on the thread that owns the window.
 */
public final class GlfwCursorSurface implements CursorSurface {
    private static final @NotNull Logger LOGGER = Logger.of(GlfwCursorSurface.class);
    private static final int RGBA_BYTES = 4;
    private static final int ALPHA_SHIFT = 24;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    /**
     * The blank cursor of {@link #hide}: one transparent pixel.
     */
    private static final @NotNull DetailSet<Picture> BLANK_PICTURE = DetailSet.of(new Picture(1, 1, PixelLayer.ofRgba(new int[1])));

    /**
     * Creates the pixels of the platform cursor of a picture at a scale: {@code scale} platform pixels per picture pixel, sampled from the
     * detail picture of the scale (a picture pixel of the detail of multiple {@code n} covers {@code scale / n} platform pixels).
     *
     * @param pictures the picture with its detail pictures
     * @param scale    the platform pixels per picture pixel, at least {@code 1}
     * @return the {@code 0xAARRGGBB} pixels, row-major, of the picture's size times the scale
     */
    static int @NotNull [] createPixels(@NotNull DetailSet<Picture> pictures, int scale) {
        Picture picture = pictures.getResource();
        Picture drawn = pictures.find(scale);
        int multiple = drawn.getWidth() / picture.getWidth();
        int width = picture.getWidth() * scale;
        int height = picture.getHeight() * scale;
        PixelLayer color = drawn.getColor();
        int[] pixels = new int[width * height];

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                pixels[row * width + column] = color.getValue(row * multiple / scale * drawn.getWidth() + column * multiple / scale);
            }
        }

        return pixels;
    }

    private static long createCursor(@NotNull DetailSet<Picture> pictures, int scale) {
        Picture picture = pictures.getResource();
        int width = picture.getWidth();
        int height = picture.getHeight();
        int[] pixels = createPixels(pictures, scale);
        ByteBuffer bytes = MemoryUtil.memAlloc(pixels.length * RGBA_BYTES);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int pixel : pixels) {
                bytes.put((byte) (pixel >>> RED_SHIFT)).put((byte) (pixel >>> GREEN_SHIFT)).put((byte) pixel).put((byte) (pixel >>> ALPHA_SHIFT));
            }

            bytes.flip();

            GLFWImage image = GLFWImage.malloc(stack);

            image.set(width * scale, height * scale, bytes);

            return GLFW.glfwCreateCursor(image, Math.clamp(picture.getAnchorX() * scale, 0, width * scale - 1),
                                         Math.clamp(picture.getAnchorY() * scale, 0, height * scale - 1));
        } finally {
            MemoryUtil.memFree(bytes);
        }
    }


    private final @NotNull GlfwHostWindow window;
    private final @NotNull Map<@NotNull DetailSet<Picture>, @NotNull Long> cursors;

    /**
     * The picture set on the window, {@link #BLANK_PICTURE} while hidden, or {@code null} while the platform's own arrow is shown.
     */
    private @Nullable DetailSet<Picture> shown;
    private int scale;
    private boolean failed;
    private boolean closed;

    /**
     * Creates a {@link GlfwCursorSurface} instance; the window keeps the platform's own arrow until a picture is shown.
     *
     * @param window the window whose pointer it shows
     */
    public GlfwCursorSurface(@NotNull GlfwHostWindow window) {
        this.window  = window;
        this.cursors = new IdentityHashMap<>();

        this.shown  = null;
        this.scale  = 1;
        this.failed = false;
        this.closed = false;
    }

    /**
     * @return {@code true} if the platform shows cursor pictures (it has not refused one), {@code false} otherwise
     */
    @Override
    public boolean isAvailable() {
        return !this.failed;
    }

    /**
     * Sets how many platform pixels each picture pixel covers, so that the pointer keeps its size on a scaled display; a picture shown already
     * is shown again at the new scale.
     *
     * @param scale the whole scale, at least {@code 1}
     * @throws IllegalArgumentException if the scale is less than {@code 1}
     */
    @Override
    public void setScale(int scale) {
        if (scale < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid cursor scale {0}", scale);
        }

        if (scale == this.scale) {
            return;
        }

        DetailSet<Picture> current = this.shown;

        this.scale = scale;
        this.destroyCursors();

        if (current != null) {
            this.shown = null;
            this.show(current);
        }
    }


    /**
     * Shows a picture as the pointer, from the detail of the scale; the picture's anchor is the hot spot. Showing the same set again builds
     * nothing new. A refused picture makes the surface unavailable ({@link #isAvailable}) and shows the platform's own arrow.
     *
     * @param pictures the picture with its detail pictures
     */
    @Override
    public void show(@NotNull DetailSet<Picture> pictures) {
        if (this.failed || this.closed || pictures == this.shown) {
            return;
        }

        Long cursor = this.cursors.get(pictures);

        if (cursor == null) {
            cursor = createCursor(pictures, this.scale);

            if (cursor == MemoryUtil.NULL) {
                LOGGER.warning("CURSOR: custom cursors are unavailable; the platform arrow is shown.");
                this.failed = true;
                this.showDefault();

                return;
            }

            this.cursors.put(pictures, cursor);
        }

        GLFW.glfwSetCursor(this.window.getHandle(), cursor);
        this.shown = pictures;
    }

    /**
     * Shows the platform's own arrow.
     */
    @Override
    public void showDefault() {
        if (this.closed) {
            return;
        }

        GLFW.glfwSetCursor(this.window.getHandle(), MemoryUtil.NULL);
        this.shown = null;
    }

    /**
     * Shows no pointer over the window (a blank picture), for frames whose cursor the client draws itself or that show none. Nothing changes
     * while the surface is unavailable.
     */
    @Override
    public void hide() {
        this.show(BLANK_PICTURE);
    }

    /**
     * Disposes resources: shows the platform's own arrow again and releases the built cursors. Closing again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.showDefault();
        this.closed = true;
        this.destroyCursors();
    }

    private void destroyCursors() {
        for (long cursor : this.cursors.values()) {
            GLFW.glfwDestroyCursor(cursor);
        }

        this.cursors.clear();
    }
}
