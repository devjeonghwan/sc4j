package org.fogofwarlabs.sc4j.launcher.launch;

import org.jetbrains.annotations.NotNull;

/**
 * Explains the exit status of a runtime process. The runtime ends with 0, 1, 2 or 3; any other status means
 * that the process ended outside the runtime's control, for example because the JVM could not start or was stopped.
 */
public enum RuntimeExitStatus {
    /**
     * Status 0: the run ended normally.
     */
    SUCCESS(0, "exit.success"),
    /**
     * Status 1: the mod failed while it ran.
     */
    RUN_FAILURE(1, "exit.run.failure"),
    /**
     * Status 2: the command line, a path or the saved settings are invalid, for example a required setting has no value.
     */
    CONFIGURATION_ERROR(2, "exit.configuration.error"),
    /**
     * Status 3: the mod could not be loaded or validated, or it refused the run (for example an unsupported installation).
     */
    MOD_FAILURE(3, "exit.mod.failure"),
    /**
     * Any other status: the process ended outside the runtime's control.
     */
    UNKNOWN(-1, "exit.unknown");

    /**
     * Finds the explanation of an exit status.
     *
     * @param status the process exit status
     * @return the matching constant, {@link #UNKNOWN} for a status the runtime does not use
     */
    public static @NotNull RuntimeExitStatus fromStatus(int status) {
        for (RuntimeExitStatus exitStatus : values()) {
            if (exitStatus != UNKNOWN && exitStatus.status == status) {
                return exitStatus;
            }
        }

        return UNKNOWN;
    }


    private final int status;
    private final @NotNull String textKey;

    /**
     * Creates a {@link RuntimeExitStatus} instance.
     *
     * @param status  the process exit status, {@code -1} for {@link #UNKNOWN}
     * @param textKey the key of the explanation in the launcher texts
     */
    RuntimeExitStatus(int status, @NotNull String textKey) {
        this.status  = status;
        this.textKey = textKey;
    }

    /**
     * @return the process exit status, {@code -1} for {@link #UNKNOWN}
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * @return the key of the explanation in the launcher texts
     */
    public @NotNull String getTextKey() {
        return this.textKey;
    }
}
