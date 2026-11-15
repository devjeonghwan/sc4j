package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the option values of a game setup in the order they were set. An option that is not set has its default.
 */
public final class GameOptions {
    /**
     * The options of a setup that sets none.
     */
    public static final @NotNull GameOptions DEFAULTS = new Builder().build();

    /**
     * Builds {@link GameOptions} instances.
     */
    public static final class Builder {
        private final @NotNull List<@NotNull GameOption<?>> options;
        private final @NotNull List<@NotNull Object> values;

        /**
         * Creates a {@link Builder} instance without values.
         */
        public Builder() {
            this.options = new ArrayList<>();
            this.values  = new ArrayList<>();
        }


        /**
         * Sets the value of an option; setting an option again replaces its value and keeps its position.
         *
         * @param <T>    the value type
         * @param option the option
         * @param value  the immutable value
         * @return this instance for chaining
         */
        public <T> @NotNull Builder set(@NotNull GameOption<T> option, @NotNull T value) {
            int index = findIndex(this.options, option);

            if (index < 0) {
                this.options.add(option);
                this.values.add(value);
            } else {
                this.values.set(index, value);
            }

            return this;
        }

        /**
         * Builds a {@link GameOptions} instance with the values set so far.
         *
         * @return the built {@link GameOptions} instance
         */
        public @NotNull GameOptions build() {
            return new GameOptions(List.copyOf(this.options), List.copyOf(this.values));
        }
    }

    private static int findIndex(@NotNull List<@NotNull GameOption<?>> options, @NotNull GameOption<?> option) {
        for (int index = 0; index < options.size(); index++) {
            if (options.get(index) == option) {
                return index;
            }
        }

        return -1;
    }

    private final @NotNull List<@NotNull GameOption<?>> options;
    private final @NotNull List<@NotNull Object> values;

    private GameOptions(@NotNull List<@NotNull GameOption<?>> options, @NotNull List<@NotNull Object> values) {
        this.options = options;
        this.values  = values;
    }

    /**
     * @return the number of options that are set
     */
    public int getCount() {
        return this.options.size();
    }

    /**
     * Returns one option that is set.
     *
     * @param index the position in the order the options were set
     * @return the option
     */
    public @NotNull GameOption<?> getOption(int index) {
        return this.options.get(index);
    }

    /**
     * Returns the value of an option.
     *
     * @param <T>    the value type
     * @param option the option
     * @return the value that is set, or the option default
     * @throws IllegalStateException if the option is required and not set
     */
    public <T> @NotNull T get(@NotNull GameOption<T> option) {
        int index = findIndex(this.options, option);
        T defaultValue = option.getDefaultValue();

        if (index >= 0) {
            return option.getType().cast(this.values.get(index));
        }

        if (defaultValue == null) {
            throw Exceptions.newIllegalStateException("The setup does not set the required option {0}.", option);
        }

        return defaultValue;
    }
}
