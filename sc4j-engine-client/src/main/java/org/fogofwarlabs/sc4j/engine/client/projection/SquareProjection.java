package org.fogofwarlabs.sc4j.engine.client.projection;

/**
 * Shows the world plane from straight above: the engine default {@link WorldProjection} of a top-down game. The projected plane is the world
 * plane (one screen pixel per world unit at zoom {@code 1}), a facing shows as itself, and the depth is the world y, so things lower on the
 * screen draw over things above them. The bounds of a map are {@code 0} to its world size on both axes.
 */
public final class SquareProjection implements WorldProjection {
    private static final int DIRECTION_MASK = DIRECTION_COUNT - 1;

    /**
     * Creates a {@link SquareProjection} instance.
     */
    public SquareProjection() {
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
        return worldX;
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
        return worldY;
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
        return projectedX;
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
        return projectedY;
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
        return worldY;
    }

    /**
     * Converts a world facing to the direction it shows on the screen, for example to pick the facing row of a sprite sheet.
     *
     * @param worldDirection the world direction, {@code 0} up to {@value #DIRECTION_COUNT} excluded, {@code 0} facing up, clockwise
     * @return the screen direction in the same units, {@code 0} facing up the screen, clockwise
     */
    @Override
    public int toScreenDirection(int worldDirection) {
        return worldDirection & DIRECTION_MASK;
    }
}
