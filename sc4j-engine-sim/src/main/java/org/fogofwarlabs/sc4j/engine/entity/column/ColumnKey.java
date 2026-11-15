package org.fogofwarlabs.sc4j.engine.entity.column;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one extension column and creates its storage. A mod declares each key once, as a constant, and registers
 * it for an entity kind; keys compare by identity.
 *
 * @param <C> the column type
 */
public final class ColumnKey<C extends Column> {
    /**
     * Creates the column of a key for one pool.
     *
     * @param <C> the column type
     */
    @FunctionalInterface
    private interface ColumnCreator<C extends Column> {
        /**
         * Creates the column.
         *
         * @param key    the key
         * @param length the number of values, the pool capacity plus one, or the type count of a content table
         * @return the created column
         */
        @NotNull C create(@NotNull ColumnKey<C> key, int length);
    }

    /**
     * Creates a {@link ColumnKey} instance of an {@code int} column.
     *
     * @param name         the column name, for diagnostics
     * @param defaultValue the default value of every entity
     * @param reset        what happens to the value when the slot is allocated again
     * @return the created {@link ColumnKey} instance
     */
    public static @NotNull ColumnKey<IntColumn> ofInt(@NotNull String name, int defaultValue, @NotNull ColumnReset reset) {
        return new ColumnKey<>(name, IntColumn.class, reset, (key, length) -> new IntColumn(key, defaultValue, length));
    }

    /**
     * Creates a {@link ColumnKey} instance of a {@code byte} column.
     *
     * @param name         the column name, for diagnostics
     * @param defaultValue the default value of every entity
     * @param reset        what happens to the value when the slot is allocated again
     * @return the created {@link ColumnKey} instance
     */
    public static @NotNull ColumnKey<ByteColumn> ofByte(@NotNull String name, byte defaultValue, @NotNull ColumnReset reset) {
        return new ColumnKey<>(name, ByteColumn.class, reset, (key, length) -> new ByteColumn(key, defaultValue, length));
    }

    /**
     * Creates a {@link ColumnKey} instance of a {@code boolean} column.
     *
     * @param name         the column name, for diagnostics
     * @param defaultValue the default value of every entity
     * @param reset        what happens to the value when the slot is allocated again
     * @return the created {@link ColumnKey} instance
     */
    public static @NotNull ColumnKey<BooleanColumn> ofBoolean(@NotNull String name, boolean defaultValue, @NotNull ColumnReset reset) {
        return new ColumnKey<>(name, BooleanColumn.class, reset, (key, length) -> new BooleanColumn(key, defaultValue, length));
    }

    private final @NotNull String name;
    private final @NotNull Class<C> columnType;
    private final @NotNull ColumnReset reset;
    private final @NotNull ColumnCreator<C> creator;

    private ColumnKey(@NotNull String name, @NotNull Class<C> columnType, @NotNull ColumnReset reset, @NotNull ColumnCreator<C> creator) {
        this.name       = name;
        this.columnType = columnType;
        this.reset      = reset;
        this.creator    = creator;
    }

    /**
     * @return the column name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the column type
     */
    public @NotNull Class<C> getColumnType() {
        return this.columnType;
    }

    /**
     * @return what happens to the value when the slot is allocated again
     */
    public @NotNull ColumnReset getReset() {
        return this.reset;
    }


    /**
     * Creates the column of this key for one pool.
     *
     * @param length the number of values, the pool capacity plus one, or the type count of a content table
     * @return the created column
     */
    @NotNull C createColumn(int length) {
        return this.creator.create(this, length);
    }

    /**
     * @return the column name and type, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.columnType.getSimpleName() + ")";
    }
}
