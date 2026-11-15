package org.fogofwarlabs.sc4j.beaconlink.protocol.message;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.beaconlink.protocol.identity.IdentityKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Rejects replayed signed messages: a message is accepted only when its timestamp lies within the window around the receiver's clock and its
 * (sender key, nonce) pair was not seen while it could still be inside the window. The cache is bounded in total and per source, the
 * receiver's own grouping of messages (for example the source address of a node's datagrams, or the sender of a client's signals): when the
 * source's share or the whole cache is full of entries that are still needed, new messages are refused ({@link ReplayCheck#SOURCE_FULL},
 * {@link ReplayCheck#FULL}) instead of forgetting an entry, so a replay can never pass. The share of a source keeps one source from filling
 * the cache for every other one.
 * <p>
 * An entry is kept for twice the window after it was accepted: its timestamp is at most one window ahead of the receiver's clock and leaves
 * the window one window later. Entries are kept in acceptance order, so expired entries are removed from the front.
 *
 * @param <S> the source type, with value equality
 */
public final class ReplayGuard<S> {
    /**
     * Holds one remembered (sender key, nonce) pair.
     */
    private static final class NonceKey {
        private final @NotNull IdentityKey sender;
        private final long nonce;

        private NonceKey(@NotNull IdentityKey sender, long nonce) {
            this.sender = sender;
            this.nonce  = nonce;
        }

        /**
         * @param other the other object
         * @return {@code true} if the other object is a pair with the same key and nonce, {@code false} otherwise
         */
        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof NonceKey key && this.nonce == key.nonce && this.sender.equals(key.sender);
        }

        /**
         * @return the hash code of the key and the nonce
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.sender, this.nonce);
        }
    }

    /**
     * Holds the source and the removal time of one remembered pair.
     *
     * @param <S> the source type
     */
    private static final class NonceEntry<S> {
        private final @NotNull S source;
        private final long removalMillis;

        private NonceEntry(@NotNull S source, long removalMillis) {
            this.source        = source;
            this.removalMillis = removalMillis;
        }
    }


    private final long windowMillis;
    private final int capacity;
    private final int sourceCapacity;

    private final @NotNull LinkedHashMap<@NotNull NonceKey, @NotNull NonceEntry<S>> entries;
    private final @NotNull HashMap<@NotNull S, @NotNull Integer> sourceSizes;

    /**
     * Creates a {@link ReplayGuard} instance whose sources may each fill the whole cache.
     *
     * @param windowMillis the largest difference between a timestamp and the receiver's clock, in milliseconds
     * @param capacity     the largest number of remembered nonces
     * @throws IllegalArgumentException if the window or the capacity is not positive
     */
    public ReplayGuard(long windowMillis, int capacity) {
        this(windowMillis, capacity, capacity);
    }

    /**
     * Creates a {@link ReplayGuard} instance.
     *
     * @param windowMillis   the largest difference between a timestamp and the receiver's clock, in milliseconds
     * @param capacity       the largest number of remembered nonces
     * @param sourceCapacity the largest number of remembered nonces of one source
     * @throws IllegalArgumentException if the window or a capacity is not positive, or the source capacity is larger than the capacity
     */
    public ReplayGuard(long windowMillis, int capacity, int sourceCapacity) {
        if (windowMillis <= 0L || sourceCapacity <= 0 || sourceCapacity > capacity) {
            throw Exceptions.newIllegalArgumentException("Invalid replay window {0}, capacity {1} or source capacity {2}.",
                                                         windowMillis, capacity, sourceCapacity);
        }

        this.windowMillis   = windowMillis;
        this.capacity       = capacity;
        this.sourceCapacity = sourceCapacity;

        this.entries     = new LinkedHashMap<>();
        this.sourceSizes = new HashMap<>();
    }

    /**
     * @return the number of remembered nonces
     */
    public int getSize() {
        return this.entries.size();
    }

    /**
     * @param source the source
     * @return the number of remembered nonces of the source
     */
    public int getSize(@NotNull S source) {
        return this.sourceSizes.getOrDefault(source, 0);
    }


    /**
     * Checks a timestamp against the window without remembering anything: a cheap check before the signature work.
     *
     * @param timestampMillis the message timestamp
     * @param nowMillis       the receiver's clock
     * @return {@code true} if the timestamp lies within the window, {@code false} otherwise
     */
    public boolean isInWindow(long timestampMillis, long nowMillis) {
        return Math.abs(timestampMillis - nowMillis) <= this.windowMillis;
    }

    /**
     * Checks a message whose signature is valid and remembers its nonce under its source when it is accepted.
     *
     * @param source          the message's source
     * @param sender          the sender's key
     * @param nonce           the message nonce
     * @param timestampMillis the message timestamp
     * @param nowMillis       the receiver's clock
     * @return the outcome
     */
    public @NotNull ReplayCheck check(@NotNull S source, @NotNull IdentityKey sender, long nonce, long timestampMillis, long nowMillis) {
        if (!this.isInWindow(timestampMillis, nowMillis)) {
            return ReplayCheck.OUTSIDE_WINDOW;
        }

        this.removeExpired(nowMillis);

        NonceKey key = new NonceKey(sender, nonce);

        if (this.entries.containsKey(key)) {
            return ReplayCheck.REPEATED;
        }

        int sourceSize = this.getSize(source);

        if (sourceSize >= this.sourceCapacity) {
            return ReplayCheck.SOURCE_FULL;
        }

        if (this.entries.size() >= this.capacity) {
            return ReplayCheck.FULL;
        }

        this.entries.put(key, new NonceEntry<>(source, nowMillis + 2 * this.windowMillis));
        this.sourceSizes.put(source, sourceSize + 1);

        return ReplayCheck.ACCEPTED;
    }

    private void removeExpired(long nowMillis) {
        Iterator<Map.Entry<NonceKey, NonceEntry<S>>> iterator = this.entries.entrySet().iterator();

        while (iterator.hasNext()) {
            NonceEntry<S> entry = iterator.next().getValue();

            if (entry.removalMillis > nowMillis) {
                break;
            }

            iterator.remove();
            this.sourceSizes.computeIfPresent(entry.source, (source, size) -> size > 1 ? size - 1 : null);
        }
    }
}
