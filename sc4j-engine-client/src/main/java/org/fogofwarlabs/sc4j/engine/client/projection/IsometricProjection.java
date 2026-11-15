package org.fogofwarlabs.sc4j.engine.client.projection;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;

/**
 * Shows the world plane turned by an eighth turn and flattened, so that a square world cell shows as a diamond: the engine default isometric
 * {@link WorldProjection}. A cell of the configured world size shows as a diamond of the configured screen width and height ({@code 64} by
 * {@code 32} for the usual 2:1 tiles). The world x axis runs down to the right on the screen, the world y axis down to the left; the world
 * origin, the map's top corner, sits at the projected origin, so the bounds of a map start left of it.
 * <p>
 * The depth is the sum of the world coordinates, which grows down the screen: of two things standing on the plane, the lower one on the screen
 * draws over the other. A facing shows turned and flattened the same way, rounded to the nearest direction.
 */
public final class IsometricProjection implements WorldProjection {
    private static final int HALF_DIVISOR = 2;
    private static final double FULL_TURN = 2 * Math.PI;

    private final int cellSize;
    private final int tileWidth;
    private final int tileHeight;

    private final float horizontalScale;
    private final float verticalScale;

    /**
     * Creates an {@link IsometricProjection} instance.
     *
     * @param cellSize   the side of the square world cell that shows as one diamond, in world units
     * @param tileWidth  the width of the diamond in screen pixels at zoom {@code 1}
     * @param tileHeight the height of the diamond in screen pixels at zoom {@code 1}
     * @throws IllegalArgumentException if a value is not positive
     */
    public IsometricProjection(int cellSize, int tileWidth, int tileHeight) {
        if (cellSize <= 0 || tileWidth <= 0 || tileHeight <= 0) {
            throw Exceptions.newIllegalArgumentException("An isometric projection requires a positive cell size and tile size: {0}, {1}x{2}",
                                                         cellSize, tileWidth, tileHeight);
        }

        this.cellSize   = cellSize;
        this.tileWidth  = tileWidth;
        this.tileHeight = tileHeight;

        this.horizontalScale = (float) tileWidth / (HALF_DIVISOR * cellSize);
        this.verticalScale   = (float) tileHeight / (HALF_DIVISOR * cellSize);
    }

    /**
     * @return the side of the square world cell that shows as one diamond, in world units
     */
    public int getCellSize() {
        return this.cellSize;
    }

    /**
     * @return the width of the diamond in screen pixels at zoom {@code 1}
     */
    public int getTileWidth() {
        return this.tileWidth;
    }

    /**
     * @return the height of the diamond in screen pixels at zoom {@code 1}
     */
    public int getTileHeight() {
        return this.tileHeight;
    }


    /**
     * Converts a world position to an x of the projected plane.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the projected x
     */
    @Override
    public float toProjectedX(float worldX, float worldY) {
        return (worldX - worldY) * this.horizontalScale;
    }

    /**
     * Converts a world position to a y of the projected plane.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the projected y
     */
    @Override
    public float toProjectedY(float worldX, float worldY) {
        return (worldX + worldY) * this.verticalScale;
    }

    /**
     * Converts a position of the projected plane to a world x.
     *
     * @param projectedX the projected x
     * @param projectedY the projected y
     * @return the world x, outside the map if the position shows no part of it
     */
    @Override
    public float toWorldX(float projectedX, float projectedY) {
        return (projectedY / this.verticalScale + projectedX / this.horizontalScale) / HALF_DIVISOR;
    }

    /**
     * Converts a position of the projected plane to a world y.
     *
     * @param projectedX the projected x
     * @param projectedY the projected y
     * @return the world y, outside the map if the position shows no part of it
     */
    @Override
    public float toWorldY(float projectedX, float projectedY) {
        return (projectedY / this.verticalScale - projectedX / this.horizontalScale) / HALF_DIVISOR;
    }

    /**
     * Reads the depth of a world position: things standing on the plane draw in increasing depth, so a larger depth draws over a smaller one.
     *
     * @param worldX the world x
     * @param worldY the world y
     * @return the depth
     */
    @Override
    public float getDepth(float worldX, float worldY) {
        return worldX + worldY;
    }

    /**
     * Converts a world facing to the direction it shows on the screen, for example to pick the facing row of a sprite sheet.
     *
     * @param worldDirection the world direction, {@code 0} up to {@value #DIRECTION_COUNT} excluded, {@code 0} facing up, clockwise
     * @return the screen direction in the same units, {@code 0} facing up the screen, clockwise
     */
    @Override
    public int toScreenDirection(int worldDirection) {
        double angle = FULL_TURN * worldDirection / DIRECTION_COUNT;
        double worldX = Math.sin(angle);
        double worldY = -Math.cos(angle);
        double screenX = (worldX - worldY) * this.horizontalScale;
        double screenY = (worldX + worldY) * this.verticalScale;
        double screenAngle = Math.atan2(screenX, -screenY);

        return Math.floorMod(Math.round(screenAngle * DIRECTION_COUNT / FULL_TURN), DIRECTION_COUNT);
    }
}
