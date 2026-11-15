package org.fogofwarlabs.sc4j.engine.state;

import org.jetbrains.annotations.NotNull;

/**
 * Accumulates the {@code 64}-bit state hash.
 * <p>
 * The hash starts at {@link #OFFSET_BASIS}. Each mixed {@code int} is taken as an unsigned {@code 32}-bit value: the
 * hash is XORed with it, multiplied by {@link #PRIME} and then XORed with itself shifted right by {@link #FOLD_SHIFT}
 * bits. A {@code long} is mixed as its low and then its high {@code 32} bits, a {@code boolean} as {@code 1} or
 * {@code 0}. The function is part of the engine contract: it does not change between engine versions of one major
 * version, so recorded hashes stay comparable.
 * <p>
 * One instance is reused for every computation ({@link #reset()}), so computing a hash allocates nothing.
 */
public final class StateHash {
    /**
     * The initial value of the hash (the {@code 64}-bit FNV offset basis).
     */
    public static final long OFFSET_BASIS = 0xcbf29ce484222325L;
    /**
     * The multiplier of each mix step (the {@code 64}-bit FNV prime).
     */
    public static final long PRIME = 0x100000001b3L;
    /**
     * The right shift folded into the hash after each mixed value.
     */
    public static final int FOLD_SHIFT = 29;

    /**
     * The mask of the unsigned {@code 32}-bit value of an {@code int}.
     */
    private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;

    private long value;

    /**
     * Creates a {@link StateHash} instance at {@link #OFFSET_BASIS}.
     */
    public StateHash() {
        this.value = OFFSET_BASIS;
    }

    /**
     * @return the hash of the values mixed since the last {@link #reset()}
     */
    public long getValue() {
        return this.value;
    }


    /**
     * Mixes an {@code int} value as an unsigned {@code 32}-bit value.
     *
     * @param mixed the value
     * @return this instance for chaining
     */
    public @NotNull StateHash mix(int mixed) {
        long hash = this.value;

        hash ^= mixed & UNSIGNED_INT_MASK;
        hash *= PRIME;
        hash ^= hash >>> FOLD_SHIFT;

        this.value = hash;

        return this;
    }

    /**
     * Mixes a {@code long} value: its low and then its high {@code 32} bits.
     *
     * @param mixed the value
     * @return this instance for chaining
     */
    public @NotNull StateHash mixLong(long mixed) {
        this.mix((int) mixed);

        return this.mix((int) (mixed >>> Integer.SIZE));
    }

    /**
     * Mixes a {@code boolean} value as {@code 1} or {@code 0}.
     *
     * @param mixed the value
     * @return this instance for chaining
     */
    public @NotNull StateHash mixBoolean(boolean mixed) {
        return this.mix(mixed ? 1 : 0);
    }

    /**
     * Mixes every value of an {@code int} array in index order, after its length.
     *
     * @param mixed the values
     * @return this instance for chaining
     */
    public @NotNull StateHash mixArray(int @NotNull [] mixed) {
        this.mix(mixed.length);

        for (int value : mixed) {
            this.mix(value);
        }

        return this;
    }

    /**
     * Starts a new hash at {@link #OFFSET_BASIS}.
     *
     * @return this instance for chaining
     */
    public @NotNull StateHash reset() {
        this.value = OFFSET_BASIS;

        return this;
    }
}
