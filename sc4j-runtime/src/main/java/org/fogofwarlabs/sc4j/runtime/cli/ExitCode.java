package org.fogofwarlabs.sc4j.runtime.cli;

/**
 * Lists the exit codes of the runtime. The launcher reads them to tell the user why a run ended.
 */
public enum ExitCode {
    /**
     * The run ended normally.
     */
    SUCCESS(0),
    /**
     * The mod failed while it ran.
     */
    RUN_FAILURE(1),
    /**
     * The command line, a path or the saved settings are invalid, for example a required setting has no value.
     */
    CONFIGURATION_ERROR(2),
    /**
     * The mod cannot be loaded or validated, or it refused the run: broken manifest, unsupported engine version, failing
     * entry class, a {@code ModException} from the mod.
     */
    MOD_FAILURE(3);

    private final int status;

    /**
     * Creates a {@link ExitCode} instance.
     *
     * @param status the process exit status
     */
    ExitCode(int status) {
        this.status = status;
    }

    /**
     * @return the process exit status
     */
    public int getStatus() {
        return this.status;
    }
}
