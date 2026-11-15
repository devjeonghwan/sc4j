package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies one option of a game setup, such as a starting resource amount or a victory condition. The engine and the
 * mods declare each option once, as a constant; options compare by identity. Option values are immutable. A required
 * option has no default: every setup of a game that reads it sets it.
 *
 * @param <T> the value type
 */
public final class GameOption<T> {
    /**
     * Creates a {@link GameOption} instance.
     *
     * @param <T>          the value type
     * @param name         the option name, for diagnostics
     * @param type         the value type, a reference type ({@code Integer.class}, not {@code int.class})
     * @param defaultValue the value of a setup that does not set the option
     * @return the created {@link GameOption} instance
     * @throws IllegalArgumentException if the type is primitive or the default value is not of the type
     */
    public static <T> @NotNull GameOption<T> of(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue) {
        if (type.isPrimitive() || !type.isInstance(defaultValue)) {
            throw Exceptions.newIllegalArgumentException("The option {0} needs a reference type that its default value has: {1}", name, type);
        }

        return new GameOption<>(name, type, defaultValue);
    }

    /**
     * Creates a {@link GameOption} instance without a default, which every setup of a game that reads it sets.
     *
     * @param <T>  the value type
     * @param name the option name, for diagnostics
     * @param type the value type, a reference type
     * @return the created {@link GameOption} instance
     * @throws IllegalArgumentException if the type is primitive
     */
    public static <T> @NotNull GameOption<T> required(@NotNull String name, @NotNull Class<T> type) {
        if (type.isPrimitive()) {
            throw Exceptions.newIllegalArgumentException("The option {0} needs a reference type: {1}", name, type);
        }

        return new GameOption<>(name, type, null);
    }

    private final @NotNull String name;
    private final @NotNull Class<T> type;
    private final @Nullable T defaultValue;

    private GameOption(@NotNull String name, @NotNull Class<T> type, @Nullable T defaultValue) {
        this.name         = name;
        this.type         = type;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the option name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the value type
     */
    public @NotNull Class<T> getType() {
        return this.type;
    }

    /**
     * @return the value of a setup that does not set the option, or {@code null} if the option is required
     */
    public @Nullable T getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * @return {@code true} if the option has no default and a setup must set it, {@code false} otherwise
     */
    public boolean isRequired() {
        return this.defaultValue == null;
    }


    /**
     * @return the option name and type, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.type.getSimpleName() + ")";
    }
}
