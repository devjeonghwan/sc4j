package org.fogofwarlabs.sc4j.beaconlink.protocol;

import org.jetbrains.annotations.NotNull;

/**
 * Supplies the wall clock time of the Beacon.Link protocol in milliseconds since the epoch: message timestamps, the replay window and the
 * expiry of registrations, rooms and known nodes are compared across machines, so the protocol uses wall clock time and tolerates a skew of
 * the replay window. Tests inject a clock they advance by hand.
 */
@FunctionalInterface
public interface ProtocolClock {
    /**
     * The clock of the running machine, {@link System#currentTimeMillis()}.
     */
    @NotNull ProtocolClock SYSTEM = System::currentTimeMillis;

    /**
     * @return the current time in milliseconds since the epoch
     */
    long getEpochMillis();
}
