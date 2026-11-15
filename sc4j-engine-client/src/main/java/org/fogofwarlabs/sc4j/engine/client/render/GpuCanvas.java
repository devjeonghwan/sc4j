package org.fogofwarlabs.sc4j.engine.client.render;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Draws the 2D interface on the GPU of a {@link GraphicsDevice}: a {@link Canvas} that also offers its textures by their device names, draws
 * into a framebuffer of another size than its drawing coordinates, and draws signed distance field glyph pages (for a {@link GlyphAtlas}).
 * <p>
 * Besides the {@link Texture} objects of the {@link Canvas} contract, the texture names ({@link #createTexture},
 * {@link #drawTexture(int, float, float, float, float, int)}) let a client whose image caches keep texture names in primitive arrays draw
 * through them without a wrapper object per image. A texture name is valid on the canvas that created it until {@link #deleteTexture}.
 */
public interface GpuCanvas extends Canvas, AutoCloseable {
    /**
     * Creates an empty one-byte signed distance field page of a glyph atlas, sampled linearly.
     *
     * @param width  the width in texels
     * @param height the height in texels
     * @return the texture name
     */
    int createSdfTexture(int width, int height);

    /**
     * Replaces a rectangle of a distance field page.
     *
     * @param texture the page texture
     * @param x       the left texel
     * @param y       the top texel
     * @param width   the width in texels
     * @param height  the height in texels
     * @param texels  the row-major texels
     */
    void updateSdfTexture(int texture, int x, int y, int width, int height, @NotNull ByteBuffer texels);

    /**
     * Uploads an RGBA image.
     *
     * @param width      the image width
     * @param height     the image height
     * @param rgbaPixels the row-major RGBA bytes, top row first
     * @return the texture name
     * @throws IllegalArgumentException if a dimension is not positive or the pixel array does not match the dimensions
     */
    int createTexture(int width, int height, byte @NotNull [] rgbaPixels);

    /**
     * Creates an empty RGBA texture for {@link #updateTexture}.
     *
     * @param width               the texture width
     * @param height              the texture height
     * @param linearMagnification whether magnification filters linearly (minification always does)
     * @return the texture name
     * @throws IllegalArgumentException if the size is not positive
     */
    int createDynamicTexture(int width, int height, boolean linearMagnification);

    /**
     * Replaces the texels of a texture created by {@link #createDynamicTexture} from packed {@code 0xRRGGBBAA} ints, uploaded without a byte
     * conversion.
     *
     * @param texture the texture name
     * @param width   the texture width
     * @param height  the texture height
     * @param pixels  the row-major texels, top row first, {@code width * height} of them
     */
    void updateTexture(int texture, int width, int height, int @NotNull [] pixels);

    /**
     * Replaces rows of a texture created by {@link #createDynamicTexture} from packed {@code 0xRRGGBBAA} ints.
     *
     * @param texture the texture name
     * @param width   the texture width
     * @param top     the first row to replace
     * @param bottom  the last row to replace
     * @param pixels  the whole texture's row-major texels, top row first
     * @throws IllegalArgumentException if the texels do not cover the rows
     */
    void updateTextureRows(int texture, int width, int top, int bottom, int @NotNull [] pixels);

    /**
     * Deletes a texture created by {@link #createTexture}.
     *
     * @param texture the texture name
     */
    void deleteTexture(int texture);

    /**
     * Begins interface drawing into a framebuffer whose pixel grid differs from the drawing coordinates: callers keep drawing in coordinates of
     * {@code coordinateWidth} by {@code coordinateHeight} (the window), which map onto the whole viewport (for example a low resolution target that
     * is scaled up to the window afterwards).
     *
     * @param framebuffer      the framebuffer, {@code 0} for the window
     * @param viewportWidth    the framebuffer width in pixels
     * @param viewportHeight   the framebuffer height in pixels
     * @param coordinateWidth  the drawing coordinates' width
     * @param coordinateHeight the drawing coordinates' height
     * @throws IllegalStateException if the renderer is closed
     */
    void begin(int framebuffer, int viewportWidth, int viewportHeight, int coordinateWidth, int coordinateHeight);

    /**
     * Draws a whole texture into a rectangle.
     *
     * @param texture the texture name
     * @param x       the left coordinate
     * @param y       the top coordinate
     * @param width   the drawn width
     * @param height  the drawn height
     * @param color   the packed color multiplied with the texture
     */
    void drawTexture(int texture, float x, float y, float width, float height, int color);

    /**
     * Draws part of a texture into a rectangle.
     *
     * @param texture the texture name
     * @param x       the left coordinate
     * @param y       the top coordinate
     * @param width   the drawn width
     * @param height  the drawn height
     * @param left    the left texture coordinate
     * @param top     the top texture coordinate
     * @param right   the right texture coordinate
     * @param bottom  the bottom texture coordinate
     * @param color   the packed color multiplied with the texture
     */
    void drawTexture(int texture, float x, float y, float width, float height,
                     float left, float top, float right, float bottom,
                     int color);

    /**
     * Draws part of a distance field page as a glyph through the distance-field shader.
     *
     * @param texture      the page texture
     * @param x            the left coordinate
     * @param y            the top coordinate
     * @param width        the drawn width
     * @param height       the drawn height
     * @param left         the left texture coordinate
     * @param top          the top texture coordinate
     * @param right        the right texture coordinate
     * @param bottom       the bottom texture coordinate
     * @param color        the packed glyph color
     * @param shadowOffset the drop shadow offset in texels right and down, or {@code 0}
     * @param shadowPower  the drop shadow weight, or {@code 0} for none
     */
    void drawSdfTexture(int texture,
                        float x, float y, float width, float height,
                        float left, float top, float right, float bottom,
                        int color,
                        float shadowOffset, float shadowPower);


    /**
     * Disposes resources: releases the GPU programs, buffers and textures and the staging memory. Closing again does nothing.
     */
    @Override
    void close();
}
