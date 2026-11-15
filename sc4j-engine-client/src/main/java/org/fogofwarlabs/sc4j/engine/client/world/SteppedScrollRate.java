package org.fogofwarlabs.sc4j.engine.client.world;

import org.jetbrains.annotations.NotNull;

/**
 * Scrolls by the steps of a {@link ScrollStepTable}: one step per update, read by the key speed row or the edge speed row and the acceleration
 * step, whatever the time and the zoom. A row outside the table reads the nearest row, and the acceleration steps are the steps of the table.
 * The table is read at every update, so a table that changes while a game runs changes the scrolling at once.
 */
public final class SteppedScrollRate implements ScrollRate {
    private final @NotNull ScrollStepTable steps;

    private int keySpeed;
    private int edgeSpeed;

    /**
     * Creates a {@link SteppedScrollRate} instance with the middle row of the table for keys and edges.
     *
     * @param steps the step table
     */
    public SteppedScrollRate(@NotNull ScrollStepTable steps) {
        this.steps = steps;

        this.keySpeed  = steps.getSpeedCount() / 2;
        this.edgeSpeed = steps.getSpeedCount() / 2;
    }

    /**
     * @return the step table
     */
    public @NotNull ScrollStepTable getSteps() {
        return this.steps;
    }

    /**
     * @return the speed row of key scrolling
     */
    public int getKeySpeed() {
        return this.keySpeed;
    }

    /**
     * Sets the speed row of key scrolling.
     *
     * @param keySpeed the row, read as the nearest row of the table
     */
    public void setKeySpeed(int keySpeed) {
        this.keySpeed = keySpeed;
    }

    /**
     * @return the speed row of edge scrolling
     */
    public int getEdgeSpeed() {
        return this.edgeSpeed;
    }

    /**
     * Sets the speed row of edge scrolling.
     *
     * @param edgeSpeed the row, read as the nearest row of the table
     */
    public void setEdgeSpeed(int edgeSpeed) {
        this.edgeSpeed = edgeSpeed;
    }

    /**
     * @return the number of acceleration steps: the step count of the table
     */
    @Override
    public int getStepCount() {
        return this.steps.getStepCount();
    }


    /**
     * Reads the distance of an update of key scrolling: the table step of the key speed row.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    @Override
    public float getKeyDistance(int step, long milliseconds, float zoom) {
        return this.steps.getStep(Math.clamp(this.keySpeed, 0, this.steps.getSpeedCount() - 1), step);
    }

    /**
     * Reads the distance of an update of edge scrolling: the table step of the edge speed row.
     *
     * @param step         the acceleration step from {@code 0} through {@link #getStepCount()} {@code - 1}
     * @param milliseconds the time of the update
     * @param zoom         the camera's screen pixels per projected pixel
     * @return the distance on each moving axis in projected pixels
     */
    @Override
    public float getEdgeDistance(int step, long milliseconds, float zoom) {
        return this.steps.getStep(Math.clamp(this.edgeSpeed, 0, this.steps.getSpeedCount() - 1), step);
    }
}
