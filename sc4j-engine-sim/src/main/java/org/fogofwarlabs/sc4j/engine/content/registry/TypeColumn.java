package org.fogofwarlabs.sc4j.engine.content.registry;

import org.fogofwarlabs.sc4j.engine.entity.column.Column;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnTable;
import org.jetbrains.annotations.NotNull;

/**
 * Holds one extension column a mod registered for a type table: its key and the initializer of its values.
 *
 * @param <C> the column type
 */
public final class TypeColumn<C extends Column> {
    private final @NotNull ColumnKey<C> key;
    private final @NotNull TypeColumnInitializer<? super C> initializer;

    /**
     * Creates a {@link TypeColumn} instance.
     *
     * @param key         the column key
     * @param initializer sets the initial values
     */
    TypeColumn(@NotNull ColumnKey<C> key, @NotNull TypeColumnInitializer<? super C> initializer) {
        this.key         = key;
        this.initializer = initializer;
    }

    /**
     * @return the column key
     */
    public @NotNull ColumnKey<C> getKey() {
        return this.key;
    }


    /**
     * Sets the initial values of this column in the extension columns of a new table.
     *
     * @param columns the extension columns of the table, which hold this key
     */
    public void initialize(@NotNull ColumnTable columns) {
        this.initializer.initialize(columns.get(this.key));
    }
}
