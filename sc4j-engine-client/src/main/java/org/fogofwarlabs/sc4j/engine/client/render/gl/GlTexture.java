package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.engine.client.render.Texture;
import org.jetbrains.annotations.NotNull;

/**
 * Holds an OpenGL texture that a {@link GlCanvas} uploaded ({@link GlCanvas#uploadTexture}).
 */
public final class GlTexture implements Texture {
    private final @NotNull GlCanvas canvas;
    private final int name;
    private final int width;
    private final int height;

    private boolean closed;

    /**
     * Creates a {@link GlTexture} instance over an uploaded texture.
     *
     * @param canvas the canvas that uploaded it
     * @param name   the OpenGL texture name
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    GlTexture(@NotNull GlCanvas canvas, int name, int width, int height) {
        this.canvas = canvas;
        this.name   = name;
        this.width  = width;
        this.height = height;

        this.closed = false;
    }

    /**
     * @return the canvas that uploaded the texture
     */
    @NotNull GlCanvas getCanvas() {
        return this.canvas;
    }

    /**
     * Returns the OpenGL texture name, for drawing through the canvas's texture name methods.
     *
     * @return the texture name
     */
    public int getName() {
        return this.name;
    }

    /**
     * @return the width in pixels
     */
    @Override
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height in pixels
     */
    @Override
    public int getHeight() {
        return this.height;
    }

    /**
     * Disposes resources: releases the uploaded image. Closing again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        this.canvas.deleteTexture(this.name);
    }
}
