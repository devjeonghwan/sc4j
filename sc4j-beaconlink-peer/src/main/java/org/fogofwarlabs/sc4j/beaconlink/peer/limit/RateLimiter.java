package org.fogofwarlabs.sc4j.beaconlink.peer.limit;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Limits the message rate per key (a source address or a sender key) with token buckets: each key may send a burst of messages, then the
 * refill rate. The table of buckets is bounded: when it is full, only buckets that have refilled completely are removed (such a bucket is
 * equal to a new one, so removing it changes nothing for its key); if none has, a new key is refused until one has.
 * <p>
 * A token is counted as many units as its refill period has milliseconds, so a refill rate in tokens per period is a whole number of units per
 * millisecond.
 *
 * @param <K> the key type, with value equality
 */
public final class RateLimiter<K> {
    private static final long MILLIS_PER_SECOND = 1000L;

    /**
     * Holds the tokens of one key.
     */
    private static final class Bucket {
        private long units;
        private long updatedMillis;

        private Bucket(long units, long updatedMillis) {
            this.units         = units;
            this.updatedMillis = updatedMillis;
        }
    }


    private final int capacity;
    private final long unitsPerToken;
    private final long burstUnits;
    private final long unitsPerMillisecond;

    private final @NotNull HashMap<@NotNull K, @NotNull Bucket> buckets;
    private long failedEvictionMillis;

    /**
     * Creates a {@link RateLimiter} instance with a refill rate in tokens per second.
     *
     * @param capacity        the largest number of keys with a bucket
     * @param burst           the tokens of a new or refilled bucket
     * @param tokensPerSecond the refill rate
     * @throws IllegalArgumentException if a value is not positive
     */
    public RateLimiter(int capacity, int burst, int tokensPerSecond) {
        this(capacity, burst, tokensPerSecond, MILLIS_PER_SECOND);
    }

    /**
     * Creates a {@link RateLimiter} instance with a refill rate in tokens per period, for rates below one token per second.
     *
     * @param capacity     the largest number of keys with a bucket
     * @param burst        the tokens of a new or refilled bucket
     * @param tokens       the tokens refilled per period
     * @param periodMillis the refill period, in milliseconds
     * @throws IllegalArgumentException if a value is not positive
     */
    public RateLimiter(int capacity, int burst, int tokens, long periodMillis) {
        if (capacity <= 0 || burst <= 0 || tokens <= 0 || periodMillis <= 0L) {
            throw Exceptions.newIllegalArgumentException("The capacity {0}, the burst {1}, the tokens {2} and the period {3} must be positive.",
                                                         capacity, burst, tokens, periodMillis);
        }

        this.capacity            = capacity;
        this.unitsPerToken       = periodMillis;
        this.burstUnits          = burst * periodMillis;
        this.unitsPerMillisecond = tokens;

        this.buckets              = new HashMap<>();
        this.failedEvictionMillis = Long.MIN_VALUE;
    }

    /**
     * @return the number of keys with a bucket
     */
    public int getSize() {
        return this.buckets.size();
    }


    /**
     * Takes one token of a key.
     *
     * @param key       the key
     * @param nowMillis the current time
     * @return {@code true} if the key had a token, {@code false} otherwise (it is over its rate or the table is full of active keys)
     */
    public boolean tryAcquire(@NotNull K key, long nowMillis) {
        Bucket bucket = this.buckets.get(key);

        if (bucket == null) {
            if (this.buckets.size() >= this.capacity && !this.evictRefilled(nowMillis)) {
                return false;
            }

            bucket = new Bucket(this.burstUnits, nowMillis);
            this.buckets.put(key, bucket);
        }

        this.refill(bucket, nowMillis);

        if (bucket.units < this.unitsPerToken) {
            return false;
        }

        bucket.units -= this.unitsPerToken;

        return true;
    }

    private boolean evictRefilled(long nowMillis) {
        // A scan that found nothing is not repeated within the same millisecond: no bucket can have refilled since.
        if (nowMillis == this.failedEvictionMillis) {
            return false;
        }

        Iterator<Bucket> values = this.buckets.values().iterator();

        while (values.hasNext()) {
            Bucket bucket = values.next();

            this.refill(bucket, nowMillis);

            if (bucket.units >= this.burstUnits) {
                values.remove();
            }
        }

        if (this.buckets.size() >= this.capacity) {
            this.failedEvictionMillis = nowMillis;

            return false;
        }

        return true;
    }

    private void refill(@NotNull Bucket bucket, long nowMillis) {
        long elapsed = nowMillis - bucket.updatedMillis;

        if (elapsed > 0L) {
            long added = elapsed >= this.burstUnits / this.unitsPerMillisecond ? this.burstUnits : elapsed * this.unitsPerMillisecond;

            bucket.units         = Math.min(this.burstUnits, bucket.units + added);
            bucket.updatedMillis = nowMillis;
        }
    }
}
