package org.fogofwarlabs.sc4j.engine.map;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a resource container the map places before the game starts: its unit type, its position in map pixels, the kind of resource it
 * holds and the amount. The neutral player of the map owns it.
 */
public final class MapResource {
    private final @NotNull UnitType type;
    private final int x;
    private final int y;
    private final @NotNull ResourceKind kind;
    private final int amount;

    /**
     * Creates a {@link MapResource} instance.
     *
     * @param type   the unit type of the container
     * @param x      the x coordinate in pixels
     * @param y      the y coordinate in pixels
     * @param kind   the kind of resource it holds
     * @param amount the amount it holds, at least {@code 1}
     * @throws IllegalArgumentException if the amount is below {@code 1}
     */
    public MapResource(@NotNull UnitType type, int x, int y, @NotNull ResourceKind kind, int amount) {
        if (amount < 1) {
            throw Exceptions.newIllegalArgumentException("Invalid amount of a map resource: {0}", amount);
        }

        this.type   = type;
        this.x      = x;
        this.y      = y;
        this.kind   = kind;
        this.amount = amount;
    }

    /**
     * @return the unit type of the container
     */
    public @NotNull UnitType getType() {
        return this.type;
    }

    /**
     * @return the x coordinate in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the y coordinate in pixels
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the kind of resource the container holds
     */
    public @NotNull ResourceKind getKind() {
        return this.kind;
    }

    /**
     * @return the amount the container holds
     */
    public int getAmount() {
        return this.amount;
    }
}
