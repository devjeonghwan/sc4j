package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Scrolls a camera toward a target position in steps of a {@link ScrollStepTable}, one step per tick: the engine's scripted camera move (a
 * "center view" of a game's script). It moves the same {@link CameraControl} as {@link CameraScroll}, in whole steps so that a scripted move
 * takes the same ticks on every client. The target is aligned down to the scroll's alignment. Each axis accelerates like a stepped
 * {@link CameraScroll} up to the last step of the table and takes a smaller step where the full one would pass the target; the position stays
 * inside the camera's range. An arrival callback runs once, when the camera reaches the target or stops moving (it is held at the end of its
 * range); starting another scroll first runs a pending callback.
 */
public final class CameraTargetScroll {
    private final int alignmentMask;
    private final int speed;
    private final @NotNull ScrollStepTable steps;

    private boolean active;
    private int targetX;
    private int targetY;
    private @Nullable Runnable callback;

    private float lastX;
    private float lastY;
    private int indexX;
    private int indexY;

    /**
     * Creates an inactive {@link CameraTargetScroll} instance.
     *
     * @param alignment the alignment of the target in pixels, a power of two
     * @param speed     the speed row of the step table
     * @param steps     the step table, read at every tick
     * @throws IllegalArgumentException if the alignment is not a positive power of two or the speed row is outside the table
     */
    public CameraTargetScroll(int alignment, int speed, @NotNull ScrollStepTable steps) {
        if (alignment <= 0 || Integer.bitCount(alignment) != 1) {
            throw Exceptions.newIllegalArgumentException("Invalid target alignment {0}", alignment);
        }

        if (speed < 0 || speed >= steps.getSpeedCount()) {
            throw Exceptions.newIllegalArgumentException("Invalid scroll speed row {0}", speed);
        }

        this.alignmentMask = -alignment;
        this.speed         = speed;
        this.steps         = steps;

        this.active   = false;
        this.targetX  = 0;
        this.targetY  = 0;
        this.callback = null;

        this.lastX  = Float.NaN;
        this.lastY  = Float.NaN;
        this.indexX = 0;
        this.indexY = 0;
    }

    /**
     * @return {@code true} if a scroll is active, {@code false} otherwise
     */
    public boolean isActive() {
        return this.active;
    }


    /**
     * Starts a scroll; a pending callback of a previous scroll runs first.
     *
     * @param x        the target camera x
     * @param y        the target camera y
     * @param callback what runs on arrival, once
     */
    public void start(int x, int y, @NotNull Runnable callback) {
        if (this.callback != null) {
            Runnable pending = this.callback;

            this.callback = null;

            pending.run();
        }

        this.active   = true;
        this.targetX  = x & this.alignmentMask;
        this.targetY  = y & this.alignmentMask;
        this.callback = callback;

        this.lastX = Float.NaN;
        this.lastY = Float.NaN;
    }

    /**
     * Runs one scroll tick: moves the camera one step toward the target and runs the arrival callback once the camera is there or stops.
     *
     * @param camera the camera
     * @return {@code true} if the active scroll applied a tick, {@code false} otherwise
     */
    public boolean tick(@NotNull CameraControl camera) {
        if (!this.active) {
            return false;
        }

        float x = camera.getX();
        float y = camera.getY();
        int directionX = (int) Math.signum(this.targetX - x);
        int directionY = (int) Math.signum(this.targetY - y);

        if (directionX == 0 && directionY == 0) {
            this.indexX = 0;
            this.indexY = 0;
        } else {
            int lastIndex = this.steps.getStepCount() - 1;

            this.indexX = Math.min(lastIndex, this.indexX + 1);
            this.indexY = Math.min(lastIndex, this.indexY + 1);
        }

        float distanceY = Math.abs(this.targetY - y);

        while (distanceY != 0 && this.indexY > 0 && this.steps.getStep(this.speed, this.indexY) > distanceY) {
            this.indexY--;
        }

        float distanceX = Math.abs(this.targetX - x);

        while (distanceX != 0 && this.indexX > 0 && this.steps.getStep(this.speed, this.indexX) > distanceX) {
            this.indexX--;
        }

        float movedX = CameraScroll.move(x, directionX, this.steps.getStep(this.speed, this.indexX), camera.getMinimumX(), camera.getMaximumX());
        float movedY = CameraScroll.move(y, directionY, this.steps.getStep(this.speed, this.indexY), camera.getMinimumY(), camera.getMaximumY());

        camera.moveTo(movedX, movedY);

        boolean stuck = movedX == this.lastX && movedY == this.lastY;
        boolean arrived = movedX == this.targetX && movedY == this.targetY;

        if (this.callback != null && (stuck || arrived)) {
            Runnable arrival = this.callback;

            this.callback = null;

            arrival.run();
        }

        this.lastX = movedX;
        this.lastY = movedY;

        return true;
    }


    /**
     * Ends the scroll.
     */
    public void finish() {
        this.active = false;
    }

    /**
     * Returns to the inactive state of a new game, dropping a pending callback of the previous game.
     */
    public void reset() {
        this.active   = false;
        this.targetX  = 0;
        this.targetY  = 0;
        this.callback = null;

        this.lastX  = Float.NaN;
        this.lastY  = Float.NaN;
        this.indexX = 0;
        this.indexY = 0;
    }
}
