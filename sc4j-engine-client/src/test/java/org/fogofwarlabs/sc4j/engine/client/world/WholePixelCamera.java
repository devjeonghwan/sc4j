package org.fogofwarlabs.sc4j.engine.client.world;

import org.jetbrains.annotations.NotNull;

/**
 * A camera of the camera scroll and jump tests: whole pixels from {@code 0} to a largest position, a fixed shown area and no zoom, as a game
 * with its own camera supplies its {@link CameraControl}. It moves to every position it is given; the scrolls keep it inside the range.
 */
final class WholePixelCamera implements CameraControl {
    private final int maximumX;
    private final int maximumY;
    private final int viewWidth;
    private final int viewHeight;

    private float x;
    private float y;

    WholePixelCamera(int x, int y, int maximumX, int maximumY, int viewWidth, int viewHeight) {
        this.maximumX   = maximumX;
        this.maximumY   = maximumY;
        this.viewWidth  = viewWidth;
        this.viewHeight = viewHeight;

        this.x = x;
        this.y = y;
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }

    @Override
    public float getMinimumX() {
        return 0;
    }

    @Override
    public float getMaximumX() {
        return this.maximumX;
    }

    @Override
    public float getMinimumY() {
        return 0;
    }

    @Override
    public float getMaximumY() {
        return this.maximumY;
    }

    @Override
    public float getViewWidth() {
        return this.viewWidth;
    }

    @Override
    public float getViewHeight() {
        return this.viewHeight;
    }

    @Override
    public float getZoom() {
        return 1;
    }

    float @NotNull [] getPosition() {
        return new float[]{this.x, this.y};
    }


    @Override
    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
