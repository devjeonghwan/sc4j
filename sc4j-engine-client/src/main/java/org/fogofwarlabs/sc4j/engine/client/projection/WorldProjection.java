package org.fogofwarlabs.sc4j.engine.client.projection;

import org.jetbrains.annotations.NotNull;

/**
 * Shows the world plane of the simulation on the screen: the client side of the engine's grid and projection abstraction. The simulation keeps
 * its world positions (integer world units, x to the right, y downward, square logical cells); a projection maps them to the projected plane,
 * the screen at zoom {@code 1} before the camera moves it, and back. It also decides the draw order of things standing on the plane (their
 * depth), the outline of a footprint on the screen, the screen direction of a world facing and the bounds the camera keeps to.
 * <p>
 * The engine defaults are the top-down {@link SquareProjection}, in which the projected plane is the world plane, and the
 * {@link IsometricProjection} (2:1 diamonds). Other layouts, staggered isometric rows for example, implement the same methods; the bound
 * methods default to the box of the four projected map corners, which fits every affine projection. A projection holds no frame state, so one
 * instance serves every frame and every layer of a client. Methods allocate nothing.
 */
public interface WorldProjection {
    /**
     * The number of directions of a full turn of the client's world facings: direction {@code 0} faces up (world y decreasing), directions grow
     * clockwise. A view converts the facings of a simulation with another number of directions ({@link #toWorldDirection}).
     */
    int DIRECTION_COUNT = 256;

    /**
     * The number of values {@link #outlineFootprint} writes: x and y of four corners.
     */
    int OUTLINE_LENGTH = 8;

    /**
     * Converts a direction of a simulation with its own number of direction steps to the nearest world direction.
     *
     * @param direction      the direction, {@code 0} facing up, clockwise
     * @param directionCount the number of direction steps of a full turn of the simulation, at least {@code 1}
     * @return the world direction, {@code 0} up to {@value #DIRECTION_COUNT} excluded
     */
    static int toWorldDirection(int direction, int directionCount) {
        return Math.floorMod(Math.floorDiv((long) direction * DIRECTION_COUNT * 2 + directionCount, 2L * directionCount), DIRECTION_COUNT);
    }

    /**
     * Converts a world position to an x of the projected plane.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the projected x
     */
    float toProjectedX(float worldX, float worldY);

    /**
     * Converts a world position to a y of the projected plane.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the projected y
     */
    float toProjectedY(float worldX, float worldY);

    /**
     * Converts a position of the projected plane to a world x.
     *
     * @param projectedX the projected x
     * @param projectedY the projected y
     * @return the world x, outside the map if the position shows no part of it
     */
    float toWorldX(float projectedX, float projectedY);

    /**
     * Converts a position of the projected plane to a world y.
     *
     * @param projectedX the projected x
     * @param projectedY the projected y
     * @return the world y, outside the map if the position shows no part of it
     */
    float toWorldY(float projectedX, float projectedY);

    /**
     * Reads the depth of a world position: things standing on the plane draw in increasing depth, so a larger depth draws over a smaller one.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the depth
     */
    float getDepth(float worldX, float worldY);

    /**
     * Writes the outline of a world rectangle on the projected plane: the projected corners of the world top-left, top-right, bottom-right and
     * bottom-left corners, in this order, as x and y pairs.
     *
     * @param left    the world x of the left edge
     * @param top     the world y of the top edge
     * @param right   the world x of the right edge
     * @param bottom  the world y of the bottom edge
     * @param corners the {@value #OUTLINE_LENGTH} values written from index {@code 0}
     * @throws IndexOutOfBoundsException if the array is shorter than {@value #OUTLINE_LENGTH}
     */
    default void outlineFootprint(float left, float top, float right, float bottom, float @NotNull [] corners) {
        corners[0] = this.toProjectedX(left, top);
        corners[1] = this.toProjectedY(left, top);
        corners[2] = this.toProjectedX(right, top);
        corners[3] = this.toProjectedY(right, top);
        corners[4] = this.toProjectedX(right, bottom);
        corners[5] = this.toProjectedY(right, bottom);
        corners[6] = this.toProjectedX(left, bottom);
        corners[7] = this.toProjectedY(left, bottom);
    }

    /**
     * Converts a world facing to the direction it shows on the screen, for example to pick the facing row of a sprite sheet.
     *
     * @param worldDirection the world direction, {@code 0} up to {@value #DIRECTION_COUNT} excluded, {@code 0} facing up, clockwise
     * @return the screen direction in the same units, {@code 0} facing up the screen, clockwise
     */
    int toScreenDirection(int worldDirection);

    /**
     * Reads the left edge of a map's bounds on the projected plane, the smallest projected x of its corners.
     *
     * @param worldWidth  the map width in world units
     * @param worldHeight the map height in world units
     * @return the left edge
     */
    default float getProjectedLeft(float worldWidth, float worldHeight) {
        return Math.min(Math.min(this.toProjectedX(0, 0), this.toProjectedX(worldWidth, 0)),
                        Math.min(this.toProjectedX(worldWidth, worldHeight), this.toProjectedX(0, worldHeight)));
    }

    /**
     * Reads the top edge of a map's bounds on the projected plane, the smallest projected y of its corners.
     *
     * @param worldWidth  the map width in world units
     * @param worldHeight the map height in world units
     * @return the top edge
     */
    default float getProjectedTop(float worldWidth, float worldHeight) {
        return Math.min(Math.min(this.toProjectedY(0, 0), this.toProjectedY(worldWidth, 0)),
                        Math.min(this.toProjectedY(worldWidth, worldHeight), this.toProjectedY(0, worldHeight)));
    }

    /**
     * Reads the right edge of a map's bounds on the projected plane, the largest projected x of its corners.
     *
     * @param worldWidth  the map width in world units
     * @param worldHeight the map height in world units
     * @return the right edge
     */
    default float getProjectedRight(float worldWidth, float worldHeight) {
        return Math.max(Math.max(this.toProjectedX(0, 0), this.toProjectedX(worldWidth, 0)),
                        Math.max(this.toProjectedX(worldWidth, worldHeight), this.toProjectedX(0, worldHeight)));
    }

    /**
     * Reads the bottom edge of a map's bounds on the projected plane, the largest projected y of its corners.
     *
     * @param worldWidth  the map width in world units
     * @param worldHeight the map height in world units
     * @return the bottom edge
     */
    default float getProjectedBottom(float worldWidth, float worldHeight) {
        return Math.max(Math.max(this.toProjectedY(0, 0), this.toProjectedY(worldWidth, 0)),
                        Math.max(this.toProjectedY(worldWidth, worldHeight), this.toProjectedY(0, worldHeight)));
    }
}
