package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the statuses of a {@link ResultMessage} with their wire codes.
 */
public enum ResultStatus {
    /**
     * The request was carried out: a room stored or withdrawn, an announcement stored (the value is its lifetime in seconds), a graft taken.
     */
    ACCEPTED(1),
    /**
     * The room advertisement is not newer than the one the node holds.
     */
    STALE(2),
    /**
     * The request is invalid: a bad advertisement signature, an expiry outside the allowed lifetime, a room of another host, an address
     * token or a proof that is not valid, a graft for a topic the peer is not in.
     */
    INVALID(3),
    /**
     * The sender has no validated registration at its source address.
     */
    NOT_REGISTERED(4),
    /**
     * A bounded store of the node is full, the host has the most rooms allowed or published too many lately, or the mesh of the topic is
     * full.
     */
    LIMIT_REACHED(5),
    /**
     * The signal was relayed to its target.
     */
    DELIVERED(6),
    /**
     * The signal's target has no registration at the node.
     */
    UNKNOWN_TARGET(7);

    private static final @NotNull ResultStatus @NotNull [] VALUES = values();

    /**
     * Finds a status by its wire code.
     *
     * @param code the wire code
     * @return the status, or {@code null} if no status has the code
     */
    public static @Nullable ResultStatus find(int code) {
        for (ResultStatus status : VALUES) {
            if (status.code == code) {
                return status;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates a {@link ResultStatus} instance.
     *
     * @param code the wire code
     */
    ResultStatus(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
