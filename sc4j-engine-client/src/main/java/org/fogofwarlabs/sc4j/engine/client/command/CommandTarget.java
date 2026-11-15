package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the target of a command the player points at: a map position in pixels and the unit there, if any.
 */
public final class CommandTarget {
    /**
     * Creates a {@link CommandTarget} instance of a map position without a unit.
     *
     * @param x the map x in pixels
     * @param y the map y in pixels
     * @return the created {@link CommandTarget} instance
     */
    public static @NotNull CommandTarget at(int x, int y) {
        return new CommandTarget(x, y, null);
    }

    /**
     * Creates a {@link CommandTarget} instance of a pointed map position and the unit shown there: a client that orders a unit at the point
     * clicked on it, not at the unit's position.
     *
     * @param x    the map x in pixels
     * @param y    the map y in pixels
     * @param unit the unit at the position, or {@code null} for the position alone
     * @return the created {@link CommandTarget} instance
     */
    public static @NotNull CommandTarget at(int x, int y, @Nullable Unit unit) {
        return new CommandTarget(x, y, unit);
    }

    /**
     * Creates a {@link CommandTarget} instance of a unit at its position.
     *
     * @param unit the unit
     * @return the created {@link CommandTarget} instance
     */
    public static @NotNull CommandTarget of(@NotNull Unit unit) {
        return new CommandTarget(unit.getPositionX(), unit.getPositionY(), unit);
    }

    private final int x;
    private final int y;
    private final @Nullable Unit unit;

    private CommandTarget(int x, int y, @Nullable Unit unit) {
        this.x    = x;
        this.y    = y;
        this.unit = unit;
    }

    /**
     * @return the map x in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the map y in pixels
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the target unit, or {@code null} for a map position
     */
    public @Nullable Unit getUnit() {
        return this.unit;
    }
}
