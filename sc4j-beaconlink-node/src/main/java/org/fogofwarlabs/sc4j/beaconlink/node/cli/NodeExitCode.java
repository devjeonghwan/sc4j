package org.fogofwarlabs.sc4j.beaconlink.node.cli;

/**
 * Lists the exit codes of the node executable.
 */
public enum NodeExitCode {
    /**
     * The node stopped normally (help printed, or stopped by the operator).
     */
    SUCCESS(0),
    /**
     * The node failed while it ran: the socket cannot be bound, the state folder cannot be read or written.
     */
    RUN_FAILURE(1),
    /**
     * The command line is invalid.
     */
    CONFIGURATION_ERROR(2);

    private final int status;

    /**
     * Creates a {@link NodeExitCode} instance.
     *
     * @param status the process exit status
     */
    NodeExitCode(int status) {
        this.status = status;
    }

    /**
     * @return the process exit status
     */
    public int getStatus() {
        return this.status;
    }
}
