package org.fogofwarlabs.sc4j.beaconlink.peer.account;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;

/**
 * Signals that an account action failed for a reason the user can act on ({@link AccountProblem}); other failures of the files are plain
 * {@link IOException}s.
 */
public final class AccountException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final @NotNull AccountProblem problem;

    /**
     * Creates a {@link AccountException} instance.
     *
     * @param problem the reason
     * @param message the message for logs
     */
    public AccountException(@NotNull AccountProblem problem, @NotNull String message) {
        super(message);

        this.problem = problem;
    }

    /**
     * @return the reason
     */
    public @NotNull AccountProblem getProblem() {
        return this.problem;
    }
}
