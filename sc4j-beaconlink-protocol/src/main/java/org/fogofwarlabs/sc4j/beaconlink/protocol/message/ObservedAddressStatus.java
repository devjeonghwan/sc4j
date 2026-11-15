package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lists the statuses of an {@link ObservedAddressMessage} with their wire codes.
 */
public enum ObservedAddressStatus {
    /**
     * The registration needs the address token of this answer: the client repeats its registration with it, which proves that it receives
     * at the observed address.
     */
    TOKEN_REQUIRED(2),
    /**
     * The client is registered at the observed address for the given lifetime.
     */
    REGISTERED(3),
    /**
     * The client's registration was removed (a registration with lifetime {@code 0}).
     */
    UNREGISTERED(4),
    /**
     * The node holds the most registrations allowed; the client is not registered.
     */
    FULL(5),
    /**
     * The node holds the most live connections of the client's key it allows; the client is not registered.
     */
    TOO_MANY_CONNECTIONS(6),
    /**
     * The registration's display name is not a valid player name, or its stamp does not bind the name to the client's key at the node's
     * difficulty; the client is not registered.
     */
    INVALID_NAME(7),
    /**
     * The client's player signed out everywhere else, naming another connection, after the client's connection started; the client is not
     * registered.
     */
    SIGNED_OUT(8);

    private static final @NotNull ObservedAddressStatus @NotNull [] VALUES = values();

    /**
     * Finds a status by its wire code.
     *
     * @param code the wire code
     * @return the status, or {@code null} if no status has the code
     */
    public static @Nullable ObservedAddressStatus find(int code) {
        for (ObservedAddressStatus status : VALUES) {
            if (status.code == code) {
                return status;
            }
        }

        return null;
    }


    private final int code;

    /**
     * Creates an {@link ObservedAddressStatus} instance.
     *
     * @param code the wire code
     */
    ObservedAddressStatus(int code) {
        this.code = code;
    }

    /**
     * @return the wire code
     */
    public int getCode() {
        return this.code;
    }
}
