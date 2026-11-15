package org.fogofwarlabs.sc4j.engine.client.audio;

/**
 * Places a sound of the map relative to the visible part of the map, without 3D audio: a sound on screen plays at full position volume, and a
 * sound off screen fades with its distance from the screen (the horizontal plus the vertical distance outside the screen edges), down to the
 * sound's own minimum volume. Its stereo position follows its horizontal place on screen.
 */
public final class SoundPositioning {
    private static final float LEFT = -1.0f;
    private static final float RIGHT = 1.0f;

    private final int edgeVolume;
    private final int falloffDistance;
    private final float stereoWidth;

    /**
     * Creates a {@link SoundPositioning} instance.
     *
     * @param edgeVolume      the volume of a sound on screen, for example {@code 100}
     * @param falloffDistance the distance from the screen at which the position volume reaches {@code 0}, at least {@code 1}
     * @param stereoWidth     the stereo position of a sound at a screen edge from {@code 0} (no panning) to {@code 1} (fully to that side)
     */
    public SoundPositioning(int edgeVolume, int falloffDistance, float stereoWidth) {
        this.edgeVolume      = edgeVolume;
        this.falloffDistance = Math.max(1, falloffDistance);
        this.stereoWidth     = Math.clamp(stereoWidth, 0.0f, RIGHT);
    }

    /**
     * Returns the volume of a sound at a map position.
     *
     * @param minimumVolume the sound's own minimum volume
     * @param x             the horizontal map position
     * @param y             the vertical map position
     * @param left          the left screen edge on the map
     * @param top           the top screen edge on the map
     * @param right         the right screen edge on the map
     * @param bottom        the bottom screen edge on the map
     * @return {@code max(minimumVolume, edgeVolume - edgeVolume * distance / falloffDistance)} in integer arithmetic
     */
    public int getVolume(int minimumVolume, int x, int y, int left, int top, int right, int bottom) {
        int distance = Math.max(0, left - x) + Math.max(0, x - right) + Math.max(0, top - y) + Math.max(0, y - bottom);

        return Math.max(minimumVolume, this.edgeVolume - this.edgeVolume * distance / this.falloffDistance);
    }

    /**
     * Returns the stereo position of a sound at a horizontal map position.
     *
     * @param x     the horizontal map position
     * @param left  the left screen edge on the map
     * @param right the right screen edge on the map
     * @return the position from {@code -stereoWidth} (at or left of the left edge) to {@code stereoWidth} (at or right of the right edge)
     */
    public float getPan(int x, int left, int right) {
        if (right <= left) {
            return 0.0f;
        }

        float place = 2.0f * (x - left) / (right - left) - RIGHT;

        return this.stereoWidth * Math.clamp(place, LEFT, RIGHT);
    }
}
