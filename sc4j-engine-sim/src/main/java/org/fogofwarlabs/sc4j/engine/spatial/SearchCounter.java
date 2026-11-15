package org.fogofwarlabs.sc4j.engine.spatial;

/**
 * Counts the unexpanded rectangle queries of a {@link SortedEdgeIndex}: each query advances the counter and stamps the units it visits with
 * the new value, so it visits a unit with two edges in its range once.
 * <p>
 * Who owns a counter decides its scope: an index may share one counter with other indexes, and the counter may outlive a game. The counter
 * starts at zero and skips zero when it wraps.
 */
public final class SearchCounter {
    private int value;

    /**
     * Creates a {@link SearchCounter} instance at zero.
     */
    public SearchCounter() {
        this.value = 0;
    }

    /**
     * @return the current counter value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Sets the value as it is, for a restored state snapshot.
     *
     * @param value the value
     */
    public void setValue(int value) {
        this.value = value;
    }


    /**
     * Advances the counter by one, where the wrap to zero becomes one.
     *
     * @return the new value, the stamp of the query
     */
    public int advance() {
        int next = this.value + 1;

        this.value = next == 0 ? 1 : next;

        return this.value;
    }
}
