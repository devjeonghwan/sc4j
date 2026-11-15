package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Scrolls by speed and time: the key speed or the edge speed in screen pixels per second for the time of an update, divided by the zoom so
 * that the speed on the screen does not depend on it. The rate does not accelerate: it has one acceleration step.
 */
public final class ContinuousScrollRate implements ScrollRate {
    /**
     * The default key scroll speed in screen pixels per second.
     */
    public static final float DEFAULT_KEY_SPEED = 900;
    /**
     * The default edge scroll speed in screen pixels per second.
     */
    public static final float DEFAULT_EDGE_SPEED = 900;

    private static final float MILLISECONDS_PER_SECOND = 1000;
    /**
     * The one acceleration step of a rate that does not accelerate.
     */
    private static final int STEP_COUNT = 1;

    private float keySpeed;
    private float edgeSpeed;

    /**
     * Creates a {@link ContinuousScrollRate} instance with the {@link #DEFAULT_KEY_SPEED} and the {@link #DEFAULT_EDGE_SPEED}.
     */
    public ContinuousScrollRate() {
        this.keySpeed  = DEFAULT_KEY_SPEED;
        this.edgeSpeed = DEFAULT_EDGE_SPEED;
    }

    /**
     * @return the key scroll speed in screen pixels per second
     */
    public float getKeySpeed() {
        return this.keySpeed;
    }

    /**
     * Sets the key scroll speed.
     *
     * @param keySpeed the speed in screen pixels per second
     * @throws IllegalArgumentException if the speed is not positive
     */
    public void setKeySpeed(float keySpeed) {
        if (!(keySpeed > 0)) {
            throw Exceptions.newIllegalArgumentException("Invalid key scroll speed {0} px/s", keySpeed);
        }

        this.keySpeed = keySpeed;
    }

    /**
     * @return the edge scroll speed in screen pixels per second
     */
    public float getEdgeSpeed() {
        return this.edgeSpeed;
    }

    /**
     * Sets the edge scroll speed.
     *
     * @param edgeSpeed the speed in screen pixels per second
     * @throws IllegalArgumentException if the speed is not positive
     */
    public void setEdgeSpeed(float edgeSpeed) {
        if (!(edgeSpeed > 0)) {
            throw Exceptions.newIllegalArgumentException("Invalid edge scroll speed {0} px/s", edgeSpeed);
        }

        this.edgeSpeed = edgeSpeed;
    }

    /**
     * @return the number of acceleration steps: {@code 1}, the rate does not accelerate
     */
    @Override
    public int getStepCount() {
        return STEP_COUNT;
    }


    /**
     * Reads the distance of an update of key scrolling: the key speed for the time of the update, divided by the zoom.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    @Override
    public float getKeyDistance(int step, long milliseconds, float zoom) {
        return this.keySpeed * milliseconds / MILLISECONDS_PER_SECOND / zoom;
    }

    /**
     * Reads the distance of an update of edge scrolling: the edge speed for the time of the update, divided by the zoom.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    @Override
    public float getEdgeDistance(int step, long milliseconds, float zoom) {
        return this.edgeSpeed * milliseconds / MILLISECONDS_PER_SECOND / zoom;
    }
}
