package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

/**
 * Lists the outcomes of {@link ReplayGuard#check}.
 */
public enum ReplayCheck {
    /**
     * The message is new; its nonce is now remembered.
     */
    ACCEPTED,
    /**
     * The timestamp is outside the replay window around the receiver's clock.
     */
    OUTSIDE_WINDOW,
    /**
     * The sender's nonce was seen inside the window: a replayed or duplicated datagram.
     */
    REPEATED,
    /**
     * The nonce cache holds the most nonces of the message's source; the message is dropped so that no replay can pass unchecked.
     */
    SOURCE_FULL,
    /**
     * The nonce cache is full; the message is dropped so that no replay can pass unchecked.
     */
    FULL
}
