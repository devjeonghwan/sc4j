package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Names how a {@link PictureRenderer} scales a picture into a rectangle.
 */
public enum PictureScaling {
    /**
     * Scales each axis to the rectangle; the aspect ratio may change.
     */
    STRETCH,
    /**
     * Keeps the aspect ratio and shows the whole picture, as large as the rectangle allows, centred; the rest of the rectangle stays as it was.
     */
    FIT,
    /**
     * Keeps the aspect ratio and covers the whole rectangle, centred; what does not fit is cut off.
     */
    FILL
}
