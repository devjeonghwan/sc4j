package org.fogofwarlabs.sc4j.base.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides factory methods for the exception types the project throws, with indexed message placeholders such as
 * {@code "Invalid frame index: {0}"}.
 */
public final class Exceptions {
    private static final @NotNull Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static final int FORMAT_CAPACITY_MARGIN = 32;

    /**
     * Formats a message by replacing each {@code {index}} placeholder with the argument at that index.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the formatted message
     */
    public static @NotNull String format(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return format(message, arguments, Exceptions::appendArgument);
    }

    /**
     * Formats a message by replacing each {@code {index}} placeholder with the argument at that index, appended by a renderer.
     * A placeholder whose index has no argument stays as written.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @param renderer  appends one argument to the formatted message
     * @return the formatted message
     */
    public static @NotNull String format(@NotNull String message, @Nullable Object @NotNull [] arguments,
                                         @NotNull BiConsumer<@NotNull StringBuilder, @Nullable Object> renderer) {
        if (arguments.length == 0) {
            return message;
        }

        Matcher matcher = PLACEHOLDER.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + FORMAT_CAPACITY_MARGIN);
        int pivot = 0;

        while (matcher.find()) {
            int argumentIndex = Integer.parseInt(matcher.group(1));

            builder.append(message, pivot, matcher.start());

            if (argumentIndex < arguments.length) {
                renderer.accept(builder, arguments[argumentIndex]);
            } else {
                builder.append(matcher.group());
            }

            pivot = matcher.end();
        }

        return builder.append(message, pivot, message.length()).toString();
    }

    private static void appendArgument(@NotNull StringBuilder builder, @Nullable Object argument) {
        builder.append(argument);
    }

    /**
     * Creates a {@link IllegalStateException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IllegalStateException} instance
     */
    public static @NotNull IllegalStateException newIllegalStateException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IllegalStateException(format(message, arguments));
    }

    /**
     * Creates a {@link IllegalStateException} instance with a formatted message and cause.
     *
     * @param cause     the cause of the exception
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IllegalStateException} instance
     */
    public static @NotNull IllegalStateException newIllegalStateException(@NotNull Throwable cause,
                                                                          @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IllegalStateException(format(message, arguments), cause);
    }

    /**
     * Creates a {@link IllegalArgumentException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IllegalArgumentException} instance
     */
    public static @NotNull IllegalArgumentException newIllegalArgumentException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IllegalArgumentException(format(message, arguments));
    }

    /**
     * Creates a {@link IllegalArgumentException} instance with a formatted message and cause.
     *
     * @param cause     the cause of the exception
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IllegalArgumentException} instance
     */
    public static @NotNull IllegalArgumentException newIllegalArgumentException(@NotNull Throwable cause,
                                                                                @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IllegalArgumentException(format(message, arguments), cause);
    }

    /**
     * Creates a {@link UnsupportedOperationException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link UnsupportedOperationException} instance
     */
    public static @NotNull UnsupportedOperationException newUnsupportedOperationException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new UnsupportedOperationException(format(message, arguments));
    }

    /**
     * Creates a {@link UnsupportedOperationException} instance with a formatted message and cause.
     *
     * @param cause     the cause of the exception
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link UnsupportedOperationException} instance
     */
    public static @NotNull UnsupportedOperationException newUnsupportedOperationException(@NotNull Throwable cause,
                                                                                          @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new UnsupportedOperationException(format(message, arguments), cause);
    }

    /**
     * Creates a {@link IndexOutOfBoundsException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IndexOutOfBoundsException} instance
     */
    public static @NotNull IndexOutOfBoundsException newIndexOutOfBoundsException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IndexOutOfBoundsException(format(message, arguments));
    }

    /**
     * Creates a {@link NullPointerException} instance without a message, as {@code Objects.requireNonNull(value)} throws it.
     *
     * @return the created {@link NullPointerException} instance
     */
    public static @NotNull NullPointerException newNullPointerException() {
        return new NullPointerException();
    }

    /**
     * Creates a {@link NullPointerException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link NullPointerException} instance
     */
    public static @NotNull NullPointerException newNullPointerException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new NullPointerException(format(message, arguments));
    }

    /**
     * Creates a {@link ArithmeticException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link ArithmeticException} instance
     */
    public static @NotNull ArithmeticException newArithmeticException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new ArithmeticException(format(message, arguments));
    }

    /**
     * Creates a {@link IOException} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IOException} instance
     */
    public static @NotNull IOException newIOException(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IOException(format(message, arguments));
    }

    /**
     * Creates a {@link IOException} instance with a formatted message and cause.
     *
     * @param cause     the cause of the exception
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link IOException} instance
     */
    public static @NotNull IOException newIOException(@NotNull Throwable cause, @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new IOException(format(message, arguments), cause);
    }

    /**
     * Creates a {@link UncheckedIOException} instance wrapping an I/O exception.
     *
     * @param cause the wrapped I/O exception
     * @return the created {@link UncheckedIOException} instance
     */
    public static @NotNull UncheckedIOException newUncheckedIOException(@NotNull IOException cause) {
        return new UncheckedIOException(cause);
    }

    /**
     * Creates a {@link UncheckedIOException} instance with a formatted message wrapping an I/O exception.
     *
     * @param cause     the wrapped I/O exception
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link UncheckedIOException} instance
     */
    public static @NotNull UncheckedIOException newUncheckedIOException(@NotNull IOException cause,
                                                                        @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new UncheckedIOException(format(message, arguments), cause);
    }

    /**
     * Creates a {@link AssertionError} instance without a detail message.
     *
     * @return the created {@link AssertionError} instance
     */
    public static @NotNull AssertionError newAssertionError() {
        return new AssertionError();
    }

    /**
     * Creates a {@link AssertionError} instance with a formatted message.
     *
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link AssertionError} instance
     */
    public static @NotNull AssertionError newAssertionError(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new AssertionError(format(message, arguments));
    }

    /**
     * Creates a {@link AssertionError} instance with a formatted message and cause.
     *
     * @param cause     the cause of the error
     * @param message   the message format
     * @param arguments the arguments used to format the message
     * @return the created {@link AssertionError} instance
     */
    public static @NotNull AssertionError newAssertionError(@NotNull Throwable cause,
                                                            @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new AssertionError(format(message, arguments), cause);
    }

    private Exceptions() {
    }
}
