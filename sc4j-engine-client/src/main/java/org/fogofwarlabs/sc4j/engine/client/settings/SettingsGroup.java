package org.fogofwarlabs.sc4j.engine.client.settings;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * One group of a settings file ({@link SettingsStore}): its keys in file order and their values as read, with typed reads that reject a value of
 * the wrong type or range. A failed read names the setting as {@code group.key} ({@link #getQualifiedName}).
 */
public final class SettingsGroup {
    private static final @NotNull String KEY_SEPARATOR = ".";

    private final @NotNull String name;
    private final @NotNull Map<String, Object> values;

    /**
     * Creates a {@link SettingsGroup} instance over the values of a group.
     *
     * @param name   the group name
     * @param values the keys and values as read, in file order
     */
    SettingsGroup(@NotNull String name, @NotNull Map<?, ?> values) {
        this.name   = name;
        this.values = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : values.entrySet()) {
            this.values.put(String.valueOf(entry.getKey()), entry.getValue());
        }
    }

    /**
     * @return the group name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the keys of the group in file order
     */
    public @NotNull List<String> getKeys() {
        return Collections.unmodifiableList(new ArrayList<>(this.values.keySet()));
    }

    /**
     * @param key the key
     * @return the setting's name as {@code group.key}, as the read failures report it
     */
    public @NotNull String getQualifiedName(@NotNull String key) {
        return this.name + KEY_SEPARATOR + key;
    }

    /**
     * Checks whether the group has a key.
     *
     * @param key the key
     * @return {@code true} if the key is present (its value may be {@code null}), {@code false} otherwise
     */
    public boolean hasValue(@NotNull String key) {
        return this.values.containsKey(key);
    }

    /**
     * Returns a value as read: a {@link String}, a {@link Long} or {@link Double} number, a {@link Boolean}, a {@link List}, a {@link Map} or
     * {@code null}.
     *
     * @param key the key
     * @return the value, or {@code null} if the value is {@code null} or the key is absent
     */
    public @Nullable Object getValue(@NotNull String key) {
        return this.values.get(key);
    }

    /**
     * Reads a {@code true} or {@code false} value.
     *
     * @param key the key
     * @return the value
     * @throws IllegalArgumentException if the value is not a boolean
     */
    public boolean getBoolean(@NotNull String key) {
        if (this.values.get(key) instanceof Boolean flag) {
            return flag;
        }

        throw Exceptions.newIllegalArgumentException("{0} must be true or false.", this.getQualifiedName(key));
    }

    /**
     * Reads a whole number in a range.
     *
     * @param key     the key
     * @param minimum the smallest allowed value
     * @param maximum the largest allowed value
     * @return the value
     * @throws IllegalArgumentException if the value is not a whole number from {@code minimum} to {@code maximum}
     */
    public int getInteger(@NotNull String key, int minimum, int maximum) {
        if (this.values.get(key) instanceof Long number && number >= minimum && number <= maximum) {
            return number.intValue();
        }

        throw Exceptions.newIllegalArgumentException("{0} must be a number from {1} to {2}.", this.getQualifiedName(key), minimum, maximum);
    }

    /**
     * Reads a text value.
     *
     * @param key the key
     * @return the value
     * @throws IllegalArgumentException if the value is not a string
     */
    public @NotNull String getString(@NotNull String key) {
        if (this.values.get(key) instanceof String text) {
            return text;
        }

        throw Exceptions.newIllegalArgumentException("{0} must be a string.", this.getQualifiedName(key));
    }
}
