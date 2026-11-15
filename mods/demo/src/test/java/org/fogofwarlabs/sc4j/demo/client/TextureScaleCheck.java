package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws on another canvas and measures how the watched pictures are drawn: a texture uploaded with the pixels of a watched picture is that
 * picture's texture, and each drawing of it counts with its scale, the physical pixels per texture pixel along each axis ({@code 1} for a
 * drawing at the texture's own resolution).
 */
final class TextureScaleCheck implements Canvas {
    private final @NotNull Canvas canvas;

    private final @NotNull Map<@NotNull String, int @NotNull []> watched;
    private final @NotNull Map<@NotNull Texture, @NotNull String> textures;
    private final @NotNull Map<@NotNull String, @NotNull Integer> drawCounts;
    private final @NotNull Map<@NotNull String, @NotNull Float> largestErrors;

    /**
     * Creates a {@link TextureScaleCheck} instance that watches nothing yet.
     *
     * @param canvas the canvas it draws on
     */
    TextureScaleCheck(@NotNull Canvas canvas) {
        this.canvas = canvas;

        this.watched       = new HashMap<>();
        this.textures      = new IdentityHashMap<>();
        this.drawCounts    = new HashMap<>();
        this.largestErrors = new HashMap<>();
    }

    /**
     * Watches the texture of a picture without team colour mask, uploaded from now on.
     *
     * @param name    the name of the picture in the results
     * @param picture the picture
     */
    void watch(@NotNull String name, @NotNull Picture picture) {
        int[] pixels = new int[picture.getColor().getLength()];

        picture.getColor().copyValues(0, pixels, 0, pixels.length);
        this.watched.put(name, pixels);
    }

    /**
     * Returns how often a watched picture was drawn.
     *
     * @param name the name of the picture
     * @return the number of drawings
     */
    int getDrawCount(@NotNull String name) {
        return this.drawCounts.getOrDefault(name, 0);
    }

    /**
     * Returns the largest difference of a scale of a watched picture's drawings from {@code 1}.
     *
     * @param name the name of the picture
     * @return the largest difference, {@code 0} if every drawing was at the texture's own resolution or none was made
     */
    float getLargestScaleError(@NotNull String name) {
        return this.largestErrors.getOrDefault(name, 0f);
    }

    /**
     * Begins drawing over the whole window, in window pixel coordinates.
     *
     * @param width  the window framebuffer width in pixels
     * @param height the window framebuffer height in pixels
     */
    @Override
    public void begin(int width, int height) {
        this.canvas.begin(width, height);
    }

    /**
     * Begins drawing over the whole window in the logical units of an interface surface: the surface's logical area maps onto the whole
     * framebuffer, and {@link #getWidth} and {@link #getHeight} are its logical size.
     *
     * @param surface the interface surface of the frame
     * @throws UnsupportedOperationException if the canvas cannot scale and the surface's units are not framebuffer pixels
     */
    @Override
    public void begin(@NotNull UiSurface surface) {
        this.canvas.begin(surface);
    }

    /**
     * @return the width of the drawing coordinates of the current drawing
     */
    @Override
    public int getWidth() {
        return this.canvas.getWidth();
    }

    /**
     * @return the height of the drawing coordinates of the current drawing
     */
    @Override
    public int getHeight() {
        return this.canvas.getHeight();
    }

    /**
     * Returns the framebuffer pixels one drawing unit covers in the current drawing, so that a drawer can pick pictures of a matching
     * resolution.
     *
     * @return the pixels per drawing unit across
     */
    @Override
    public float getPixelScale() {
        return this.canvas.getPixelScale();
    }

    /**
     * Sets the rectangle the following drawing is limited to, or lifts the limit.
     *
     * @param x      the left coordinate
     * @param y      the top coordinate
     * @param width  the width, or a negative value to draw over the whole target again
     * @param height the height
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        this.canvas.setClip(x, y, width, height);
    }

    /**
     * Uploads an image as a texture of this canvas; the texture of a watched picture's pixels is that picture's texture.
     *
     * @param width  the image width in pixels
     * @param height the image height in pixels
     * @param colors the row-major pixels, top row first, packed {@code 0xAARRGGBB} with straight alpha as the resource model
     *               stores them
     * @return the created {@link Texture} instance, closed by the caller
     * @throws IllegalArgumentException if a dimension is not positive or the pixels do not match the dimensions
     */
    @Override
    public @NotNull Texture uploadTexture(int width, int height, int @NotNull [] colors) {
        Texture texture = this.canvas.uploadTexture(width, height, colors);

        for (Map.Entry<String, int[]> entry : this.watched.entrySet()) {
            if (Arrays.equals(entry.getValue(), colors)) {
                this.textures.put(texture, entry.getKey());
            }
        }

        return texture;
    }

    /**
     * Draws a whole texture into a rectangle.
     *
     * @param texture the texture of this canvas
     * @param x       the left coordinate
     * @param y       the top coordinate
     * @param width   the drawn width
     * @param height  the drawn height
     * @param color   the packed colour multiplied with the texture
     */
    @Override
    public void drawTexture(@NotNull Texture texture, float x, float y, float width, float height, int color) {
        this.measure(texture, width, height, 1, 1);
        this.canvas.drawTexture(texture, x, y, width, height, color);
    }

    /**
     * Draws part of a texture into a rectangle.
     *
     * @param texture the texture of this canvas
     * @param x       the left coordinate
     * @param y       the top coordinate
     * @param width   the drawn width
     * @param height  the drawn height
     * @param left    the left texture coordinate, {@code 0} to {@code 1}
     * @param top     the top texture coordinate
     * @param right   the right texture coordinate
     * @param bottom  the bottom texture coordinate
     * @param color   the packed colour multiplied with the texture
     */
    @Override
    public void drawTexture(@NotNull Texture texture, float x, float y, float width, float height,
                            float left, float top, float right, float bottom,
                            int color) {
        this.measure(texture, width, height, right - left, bottom - top);
        this.canvas.drawTexture(texture, x, y, width, height, left, top, right, bottom, color);
    }

    private void measure(@NotNull Texture texture, float width, float height, float shownWidth, float shownHeight) {
        String name = this.textures.get(texture);

        if (name == null) {
            return;
        }

        float scaleX = width * this.canvas.getPixelScale() / (shownWidth * texture.getWidth());
        float scaleY = height * this.canvas.getPixelScale() / (shownHeight * texture.getHeight());
        float error = Math.max(Math.abs(scaleX - 1), Math.abs(scaleY - 1));
        Float largest = this.largestErrors.get(name);

        this.drawCounts.merge(name, 1, Integer::sum);
        this.largestErrors.put(name, largest == null ? error : Math.max(largest, error));
    }

    /**
     * Fills a rectangle.
     *
     * @param x      the left coordinate
     * @param y      the top coordinate
     * @param width  the width
     * @param height the height
     * @param color  the packed colour
     */
    @Override
    public void fillRectangle(float x, float y, float width, float height, int color) {
        this.canvas.fillRectangle(x, y, width, height, color);
    }

    /**
     * Draws a rectangle outline inside the rectangle.
     *
     * @param x         the left coordinate
     * @param y         the top coordinate
     * @param width     the width
     * @param height    the height
     * @param thickness the line thickness
     * @param color     the packed colour
     */
    @Override
    public void drawRectangle(float x, float y, float width, float height, float thickness, int color) {
        this.canvas.drawRectangle(x, y, width, height, thickness, color);
    }

    /**
     * Draws a straight line of the given thickness.
     *
     * @param sourceX      the start x
     * @param sourceY      the start y
     * @param destinationX the end x
     * @param destinationY the end y
     * @param thickness    the line thickness
     * @param color        the packed colour
     */
    @Override
    public void drawLine(float sourceX, float sourceY, float destinationX, float destinationY, float thickness, int color) {
        this.canvas.drawLine(sourceX, sourceY, destinationX, destinationY, thickness, color);
    }

    /**
     * Submits the pending drawing.
     */
    @Override
    public void end() {
        this.canvas.end();
    }
}
