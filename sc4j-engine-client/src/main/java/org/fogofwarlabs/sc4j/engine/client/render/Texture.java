package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Holds an image uploaded to a {@link Canvas}; it is drawn only by the canvas that created it.
 */
public interface Texture extends AutoCloseable {
    /**
     * @return the width in pixels
     */
    int getWidth();

    /**
     * @return the height in pixels
     */
    int getHeight();

    /**
     * Disposes resources: releases the uploaded image. Closing again does nothing.
     */
    @Override
    void close();
}
