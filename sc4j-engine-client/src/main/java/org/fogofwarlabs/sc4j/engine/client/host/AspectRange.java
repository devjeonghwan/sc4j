package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Describes a logical screen of a fixed height whose width follows the window's aspect ratio within a range: a window
 * narrower than the minimum ratio or wider than the maximum ratio gets the logical width of that bound. The logical
 * screen is fitted into the window with one uniform scale ({@link org.fogofwarlabs.sc4j.engine.client.display.LetterboxFit}).
 */
public final class AspectRange {
    private final int logicalHeight;
    private final float minimumRatio;
    private final float maximumRatio;

    /**
     * Creates an {@link AspectRange} instance.
     *
     * @param logicalHeight the height of the logical screen in logical pixels
     * @param minimumRatio  the narrowest width-to-height ratio
     * @param maximumRatio  the widest width-to-height ratio, at least the narrowest
     * @throws IllegalArgumentException if the height or a ratio is not positive or the ratios are reversed
     */
    public AspectRange(int logicalHeight, float minimumRatio, float maximumRatio) {
        if (logicalHeight <= 0 || !(minimumRatio > 0) || !(maximumRatio >= minimumRatio)) {
            throw Exceptions.newIllegalArgumentException("Invalid aspect range: height {0}, ratios {1} to {2}", logicalHeight,
                                                         minimumRatio, maximumRatio);
        }

        this.logicalHeight = logicalHeight;
        this.minimumRatio  = minimumRatio;
        this.maximumRatio  = maximumRatio;
    }

    /**
     * @return the height of the logical screen in logical pixels
     */
    public int getLogicalHeight() {
        return this.logicalHeight;
    }

    /**
     * @return the narrowest width-to-height ratio
     */
    public float getMinimumRatio() {
        return this.minimumRatio;
    }

    /**
     * @return the widest width-to-height ratio
     */
    public float getMaximumRatio() {
        return this.maximumRatio;
    }

    /**
     * Calculates the logical width of a window: the window's aspect ratio clamped to the range times the logical height,
     * truncated to an integer. A window without height gets the width of the narrowest ratio.
     *
     * @param width  the window width
     * @param height the window height
     * @return the logical width in logical pixels
     */
    public int getLogicalWidth(float width, float height) {
        if (height <= 0) {
            return (int) (this.minimumRatio * this.logicalHeight);
        }

        return (int) (Math.clamp(width / height, this.minimumRatio, this.maximumRatio) * this.logicalHeight);
    }
}
