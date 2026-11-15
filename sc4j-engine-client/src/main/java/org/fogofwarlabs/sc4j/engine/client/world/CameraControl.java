package org.fogofwarlabs.sc4j.engine.client.world;

/**
 * Moves a world camera for the camera scrolls and jumps ({@link CameraScroll}, {@link CameraTargetScroll}, {@link ViewJumps}): the hook
 * between them and the camera of a client. The position is the top-left corner of the shown area on the projected plane of the client's
 * projection, in projected pixels, between a smallest and a largest position on each axis. The engine {@link Camera} is one; a game with its
 * own camera supplies its own.
 */
public interface CameraControl {
    /**
     * @return the projected x shown at the left edge
     */
    float getX();

    /**
     * @return the projected y shown at the top edge
     */
    float getY();

    /**
     * @return the smallest x of the position
     */
    float getMinimumX();

    /**
     * @return the largest x of the position; below {@link #getMinimumX} the camera has no room to move on the axis
     */
    float getMaximumX();

    /**
     * @return the smallest y of the position
     */
    float getMinimumY();

    /**
     * @return the largest y of the position; below {@link #getMinimumY} the camera has no room to move on the axis
     */
    float getMaximumY();

    /**
     * @return the width of the shown area in projected pixels
     */
    float getViewWidth();

    /**
     * @return the height of the shown area in projected pixels
     */
    float getViewHeight();

    /**
     * @return the screen pixels per projected pixel
     */
    float getZoom();


    /**
     * Moves the camera to a position of the top-left corner; the camera keeps itself inside the map as it does for every move.
     *
     * @param x the projected x shown at the left edge
     * @param y the projected y shown at the top edge
     */
    void moveTo(float x, float y);
}
