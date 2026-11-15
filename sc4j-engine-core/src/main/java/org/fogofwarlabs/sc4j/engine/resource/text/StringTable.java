package org.fogofwarlabs.sc4j.engine.resource.text;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds the strings of one table in index order, for example the interface texts of one language. Content refers to a
 * string by its index.
 */
public final class StringTable {
    private final @NotNull List<@NotNull String> strings;

    /**
     * Creates a {@link StringTable} instance.
     *
     * @param strings the strings in index order, copied
     */
    public StringTable(@NotNull List<@NotNull String> strings) {
        this.strings = List.copyOf(strings);
    }

    /**
     * @return the number of strings
     */
    public int getSize() {
        return this.strings.size();
    }

    /**
     * Returns one string.
     *
     * @param index the string index, {@code 0} up to {@link #getSize()} minus one
     * @return the string
     * @throws IndexOutOfBoundsException if the index names no string
     */
    public @NotNull String get(int index) {
        return this.strings.get(index);
    }

    /**
     * @return every string in index order, unmodifiable
     */
    public @NotNull List<@NotNull String> getStrings() {
        return this.strings;
    }
}
