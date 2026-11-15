package org.fogofwarlabs.sc4j.runtime.session;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.runtime.cli.ExitCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Ends a runtime session early with an exit code and the reason for the log.
 */
final class SessionFailure extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link SessionFailure} instance with a formatted message.
     *
     * @param exitCode  the exit code of the session
     * @param cause     the cause, or {@code null}
     * @param message   the message format with {@code {index}} placeholders
     * @param arguments the placeholder arguments
     * @return the created {@link SessionFailure} instance
     */
    static @NotNull SessionFailure create(@NotNull ExitCode exitCode, @Nullable Throwable cause, @NotNull String message,
                                          @Nullable Object @NotNull ... arguments) {
        return new SessionFailure(exitCode, Exceptions.format(message, arguments), cause);
    }


    private final @NotNull ExitCode exitCode;

    /**
     * Creates a {@link SessionFailure} instance.
     *
     * @param exitCode the exit code of the session
     * @param message  the formatted message
     * @param cause    the cause, or {@code null}
     */
    private SessionFailure(@NotNull ExitCode exitCode, @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);

        this.exitCode = exitCode;
    }

    /**
     * @return the exit code of the session
     */
    @NotNull ExitCode getExitCode() {
        return this.exitCode;
    }
}
