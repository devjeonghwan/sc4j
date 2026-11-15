package org.fogofwarlabs.sc4j.beaconlink.client;

/**
 * Limits the datagrams a participant passes on for one relayed pair (from one participant to another): a token bucket of
 * {@value #BURST_DATAGRAMS} datagrams that refills {@value #DATAGRAMS_PER_SECOND} datagrams a second on the protocol clock. A relay that is
 * out of tokens drops the datagram like a lost one, so that no participant can make another one send without bound.
 */
final class RelayBudget {
    /**
     * The datagrams a relayed pair may send at once.
     */
    static final int BURST_DATAGRAMS = 4096;
    /**
     * The datagrams a relayed pair may send a second over time.
     */
    static final int DATAGRAMS_PER_SECOND = 2048;

    private static final long MILLIS_PER_SECOND = 1000L;

    private long tokenMillis;
    private long lastMillis;

    /**
     * Creates a full {@link RelayBudget} instance.
     *
     * @param nowMillis the protocol clock time
     */
    RelayBudget(long nowMillis) {
        this.tokenMillis = BURST_DATAGRAMS * MILLIS_PER_SECOND;
        this.lastMillis  = nowMillis;
    }

    /**
     * Takes one datagram from the budget if the bucket holds one.
     *
     * @param nowMillis the protocol clock time
     * @return {@code true} if the datagram may be passed on, {@code false} otherwise (it is dropped)
     */
    boolean take(long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - this.lastMillis);

        // Tokens are counted in thousandths, so that the refill per millisecond stays an integer.
        this.tokenMillis = Math.min(BURST_DATAGRAMS * MILLIS_PER_SECOND, this.tokenMillis + elapsed * DATAGRAMS_PER_SECOND);
        this.lastMillis  = nowMillis;

        if (this.tokenMillis < MILLIS_PER_SECOND) {
            return false;
        }

        this.tokenMillis -= MILLIS_PER_SECOND;

        return true;
    }
}
