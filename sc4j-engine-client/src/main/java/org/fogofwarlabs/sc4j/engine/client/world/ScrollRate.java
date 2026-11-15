package org.fogofwarlabs.sc4j.engine.client.world;

/**
 * Gives the distance of one update of a {@link CameraScroll}: the speed hook that makes the one scroll component stepped or continuous. A
 * stepped rate ({@link SteppedScrollRate}) takes one step of a step table per update, so the caller updates the scroll at its tick rate; a
 * continuous rate ({@link ContinuousScrollRate}) moves by speed and the time of the update, so the caller updates it every frame.
 * <p>
 * The step is the acceleration of the scroll: {@code 0} on an update without movement, then one more on each moving update up to the last
 * step of the rate ({@link #getStepCount()} {@code - 1}).
 */
public interface ScrollRate {
    /**
     * @return the number of acceleration steps, at least {@code 1}
     */
    int getStepCount();


    /**
     * Reads the distance of an update of key scrolling.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    float getKeyDistance(int step, long milliseconds, float zoom);

    /**
     * Reads the distance of an update of edge scrolling.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    float getEdgeDistance(int step, long milliseconds, float zoom);
}
