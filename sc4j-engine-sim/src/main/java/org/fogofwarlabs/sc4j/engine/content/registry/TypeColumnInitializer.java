package org.fogofwarlabs.sc4j.engine.content.registry;

import org.fogofwarlabs.sc4j.engine.entity.column.Column;
import org.jetbrains.annotations.NotNull;

/**
 * Sets the initial values of one extension column of a type table, indexed by the type index, when a game creates the
 * table.
 * <p>
 * Determinism contract: the initializer writes the same values for every game of a definition, from data the mod
 * loaded before the definition was built; it draws no random value and reads no other game state.
 *
 * @param <C> the column type
 */
@FunctionalInterface
public interface TypeColumnInitializer<C extends Column> {
    /**
     * Sets the initial values of the column.
     *
     * @param column the new column, every value at the column default; its length is the type count of the table
     */
    void initialize(@NotNull C column);
}
