package org.fogofwarlabs.sc4j.base.log;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.function.Supplier;

/**
 * Writes the messages of one class through the process-wide {@link Logging} configuration.
 * <p>
 * Messages take indexed placeholders, as {@code "Loaded {0} in {1} ms"}; arguments are only formatted when the level is
 * enabled. Create one logger per class: {@code private static final @NotNull Logger LOGGER = Logger.of(Example.class);}.
 */
public final class Logger {
    /**
     * Creates a {@link Logger} instance for a class, named after the class's simple name.
     *
     * @param sourceType the class whose messages the logger writes
     * @return the created {@link Logger} instance
     */
    public static @NotNull Logger of(@NotNull Class<?> sourceType) {
        return new Logger(sourceType.getSimpleName());
    }

    private static void append(@NotNull StringBuilder builder, @Nullable Object value) {
        if (value != null && value.getClass().isArray()) {
            builder.append('[');

            for (int index = 0; index < Array.getLength(value); index++) {
                if (index != 0) {
                    builder.append(", ");
                }

                append(builder, Array.get(value, index));
            }

            builder.append(']');
        } else {
            builder.append(value);
        }
    }


    private final @NotNull String name;

    private Logger(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return the logger name written in log lines
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @param level the message level
     * @return {@code true} if messages of the level are written, {@code false} otherwise
     */
    public boolean isEnabled(@NotNull LogLevel level) {
        return Logging.isEnabled(level);
    }


    /**
     * Writes a message at a level.
     *
     * @param level     the message level
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Object @NotNull ... arguments) {
        if (Logging.isEnabled(level)) {
            Logging.write(level, this.name, Exceptions.format(message, arguments, Logger::append));
        }
    }

    /**
     * Writes a message built only when the level is enabled.
     *
     * @param level   the message level
     * @param message supplies the message text
     */
    public void log(@NotNull LogLevel level, @NotNull Supplier<@NotNull String> message) {
        if (Logging.isEnabled(level)) {
            Logging.write(level, this.name, message.get());
        }
    }

    /**
     * Writes an error message.
     *
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void error(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        this.log(LogLevel.ERROR, message, arguments);
    }

    /**
     * Writes a warning message.
     *
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void warning(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        this.log(LogLevel.WARNING, message, arguments);
    }

    /**
     * Writes an informational message.
     *
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void info(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        this.log(LogLevel.INFO, message, arguments);
    }

    /**
     * Writes a debug message built only when debug messages are enabled.
     *
     * @param message supplies the message text
     */
    public void debug(@NotNull Supplier<@NotNull String> message) {
        this.log(LogLevel.DEBUG, message);
    }

    /**
     * Writes a debug message.
     *
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void debug(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        this.log(LogLevel.DEBUG, message, arguments);
    }

    /**
     * Writes a trace message.
     *
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void trace(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        this.log(LogLevel.TRACE, message, arguments);
    }

    /**
     * Writes a message and the stack trace of a throwable at a level.
     *
     * @param level     the message level
     * @param throwable the throwable whose stack trace follows the message
     * @param message   the message with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     */
    public void exception(@NotNull LogLevel level, @NotNull Throwable throwable, @NotNull String message, @Nullable Object @NotNull ... arguments) {
        if (!Logging.isEnabled(level)) {
            return;
        }

        StringWriter trace = new StringWriter();

        throwable.printStackTrace(new PrintWriter(trace));
        Logging.write(level, this.name, Exceptions.format(message, arguments, Logger::append) + System.lineSeparator() +
                                        trace.toString().stripTrailing());
    }
}
