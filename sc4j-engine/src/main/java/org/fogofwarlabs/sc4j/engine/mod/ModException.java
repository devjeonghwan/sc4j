package org.fogofwarlabs.sc4j.engine.mod;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Signals that a mod cannot run: the engine cannot load it, or the mod refuses the run, for example because the external
 * data it needs is missing or of an unsupported version. The runtime ends with its mod failure exit code.
 */
public final class ModException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link ModException} instance with a formatted message.
     *
     * @param message   the message format with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     * @return the created {@link ModException} instance
     */
    public static @NotNull ModException create(@NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new ModException(Exceptions.format(message, arguments), null);
    }

    /**
     * Creates a {@link ModException} instance with a cause and a formatted message.
     *
     * @param cause     the cause
     * @param message   the message format with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     * @return the created {@link ModException} instance
     */
    public static @NotNull ModException create(@NotNull Throwable cause, @NotNull String message, @Nullable Object @NotNull ... arguments) {
        return new ModException(Exceptions.format(message, arguments), cause);
    }


    /**
     * Creates a {@link ModException} instance.
     *
     * @param message the formatted message
     * @param cause   the cause, or {@code null}
     */
    private ModException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
