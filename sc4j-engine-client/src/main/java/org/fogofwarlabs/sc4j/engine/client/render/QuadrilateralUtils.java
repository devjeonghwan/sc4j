package org.fogofwarlabs.sc4j.engine.client.render;

import org.jetbrains.annotations.NotNull;

/**
 * Reads and fills quadrilaterals given as the x and y of four corners in order around the outline: the axis-aligned rectangle check and the
 * box of the corners that {@link Canvas#fillQuadrilateral} and {@link Canvas#drawQuadrilateral} use, and the row filling of their default for
 * canvases that draw no triangles.
 */
public final class QuadrilateralUtils {
    /**
     * The number of values of a quadrilateral's corners: x and y of four corners.
     */
    public static final int CORNER_VALUES = 8;

    private static final float PIXEL_CENTER = 0.5f;
    private static final float CORNER_COUNT = 4;
    private static final int COORDINATES_PER_CORNER = 2;
    private static final int FIRST_X = 0;
    private static final int FIRST_Y = 1;
    private static final int SECOND_X = 2;
    private static final int SECOND_Y = 3;
    private static final int THIRD_X = 4;
    private static final int THIRD_Y = 5;
    private static final int LAST_X = 6;
    private static final int LAST_Y = 7;

    /**
     * Checks whether a quadrilateral is an axis-aligned rectangle: its first and last corners share the x, its second and third corners share
     * the x, its first and second corners share the y and its third and last corners share the y.
     *
     * @param corners the x and y of the four corners in order around the outline
     * @return {@code true} if the quadrilateral is an axis-aligned rectangle, {@code false} otherwise
     * @throws IndexOutOfBoundsException if the array is shorter than {@value #CORNER_VALUES}
     */
    public static boolean isRectangle(float @NotNull [] corners) {
        return corners[FIRST_X] == corners[LAST_X] && corners[SECOND_X] == corners[THIRD_X] &&
               corners[FIRST_Y] == corners[SECOND_Y] && corners[THIRD_Y] == corners[LAST_Y];
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the smallest x of the corners
     */
    public static float getLeft(float @NotNull [] corners) {
        return Math.min(Math.min(corners[FIRST_X], corners[SECOND_X]), Math.min(corners[THIRD_X], corners[LAST_X]));
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the smallest y of the corners
     */
    public static float getTop(float @NotNull [] corners) {
        return Math.min(Math.min(corners[FIRST_Y], corners[SECOND_Y]), Math.min(corners[THIRD_Y], corners[LAST_Y]));
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the largest x of the corners
     */
    public static float getRight(float @NotNull [] corners) {
        return Math.max(Math.max(corners[FIRST_X], corners[SECOND_X]), Math.max(corners[THIRD_X], corners[LAST_X]));
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the largest y of the corners
     */
    public static float getBottom(float @NotNull [] corners) {
        return Math.max(Math.max(corners[FIRST_Y], corners[SECOND_Y]), Math.max(corners[THIRD_Y], corners[LAST_Y]));
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the width of the box of the corners
     */
    public static float getWidth(float @NotNull [] corners) {
        return getRight(corners) - getLeft(corners);
    }

    /**
     * @param corners the x and y of the four corners in order around the outline
     * @return the height of the box of the corners
     */
    public static float getHeight(float @NotNull [] corners) {
        return getBottom(corners) - getTop(corners);
    }

    /**
     * Checks whether the box of a quadrilateral's corners overlaps a rectangle.
     *
     * @param corners the x and y of the four corners in order around the outline
     * @param left    the left edge of the rectangle
     * @param top     the top edge of the rectangle
     * @param right   the right edge of the rectangle
     * @param bottom  the bottom edge of the rectangle
     * @return {@code true} if the box overlaps the rectangle or touches it, {@code false} otherwise
     */
    public static boolean overlaps(float @NotNull [] corners, float left, float top, float right, float bottom) {
        return getRight(corners) >= left && getLeft(corners) <= right && getBottom(corners) >= top && getTop(corners) <= bottom;
    }

    /**
     * Moves the corners of a quadrilateral away from their centre by an amount along each axis on which a corner lies off the centre, so the
     * quadrilateral grows on every side: a rectangle by the amount, a diamond by at least the amount times its smaller half-diagonal over its
     * side. A fill of the grown outline covers the pixels a picture drawn in the box of the outline shows at its edges.
     *
     * @param corners the x and y of the four corners in order around the outline, changed in place
     * @param amount  the distance each corner moves along each axis
     * @throws IndexOutOfBoundsException if the array is shorter than {@value #CORNER_VALUES}
     */
    public static void expand(float @NotNull [] corners, float amount) {
        float centerX = (corners[FIRST_X] + corners[SECOND_X] + corners[THIRD_X] + corners[LAST_X]) / CORNER_COUNT;
        float centerY = (corners[FIRST_Y] + corners[SECOND_Y] + corners[THIRD_Y] + corners[LAST_Y]) / CORNER_COUNT;

        for (int index = 0; index < CORNER_VALUES; index += COORDINATES_PER_CORNER) {
            corners[index] += Math.signum(corners[index] - centerX) * amount;
            corners[index + 1] += Math.signum(corners[index + 1] - centerY) * amount;
        }
    }

    /**
     * Fills a convex quadrilateral row by row: every pixel row whose centre lies inside the outline gets one rectangle from the left to the
     * right crossing of the outline at the row's centre.
     *
     * @param canvas  the canvas drawn on
     * @param corners the x and y of the four corners in order around the outline
     * @param color   the packed colour
     * @throws IndexOutOfBoundsException if the array is shorter than {@value #CORNER_VALUES}
     */
    public static void fillRows(@NotNull Canvas canvas, float @NotNull [] corners, int color) {
        float top = Float.POSITIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;

        for (int index = 1; index < CORNER_VALUES; index += COORDINATES_PER_CORNER) {
            top    = Math.min(top, corners[index]);
            bottom = Math.max(bottom, corners[index]);
        }

        for (float row = (float) Math.floor(top); row < bottom; row++) {
            float center = row + PIXEL_CENTER;
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;

            for (int index = 0; index < CORNER_VALUES; index += COORDINATES_PER_CORNER) {
                int next = (index + COORDINATES_PER_CORNER) % CORNER_VALUES;
                float startY = corners[index + 1];
                float endY = corners[next + 1];

                if (startY == endY || center < Math.min(startY, endY) || center > Math.max(startY, endY)) {
                    continue;
                }

                float crossing = corners[index] + (center - startY) * (corners[next] - corners[index]) / (endY - startY);

                left  = Math.min(left, crossing);
                right = Math.max(right, crossing);
            }

            if (right > left) {
                canvas.fillRectangle(left, row, right - left, 1, color);
            }
        }
    }

    private QuadrilateralUtils() {
    }
}
