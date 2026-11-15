package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Names how a {@link PictureRenderer} fills the edges and the centre of a picture sliced by its borders into a rectangle.
 */
public enum SliceFill {
    /**
     * Stretches each edge along its side and the centre over the rest.
     */
    STRETCH,
    /**
     * Repeats each edge along its side and the centre over the rest at the picture's own size, the last repeat cut at the far side; nothing is
     * scaled except corners that the rectangle is too small for.
     */
    TILE
}
