package org.fogofwarlabs.sc4j.engine.net;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Supplies the time of the network's timeouts and resends in milliseconds. The time only moves forward; its origin is arbitrary. Tests inject
 * a clock they advance by hand, so that waiting and resending need no real sleep.
 */
@FunctionalInterface
public interface NetworkClock {
    /**
     * The clock of the running machine, the monotonic system timer.
     */
    @NotNull NetworkClock SYSTEM = () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

    /**
     * @return the current time in milliseconds from an arbitrary origin
     */
    long getMillis();
}
