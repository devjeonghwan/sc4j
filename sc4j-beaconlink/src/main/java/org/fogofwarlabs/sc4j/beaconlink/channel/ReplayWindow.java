package org.fogofwarlabs.sc4j.beaconlink.channel;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Rejects repeated and too old datagram counters of one channel direction: it remembers the highest counter accepted and which of the
 * {@value #SIZE} counters below it were accepted. A counter is fresh when it is higher than every accepted one, or lies within the window and
 * was not accepted before; anything older than the window is rejected, so a datagram can be accepted at most once.
 * <p>
 * The receiver checks a counter with {@link #isFresh} before the authentication work and records it with {@link #accept} only after the
 * datagram authenticated, so a forged datagram cannot move the window.
 */
public final class ReplayWindow {
    /**
     * The number of counters below the highest accepted one that are remembered.
     */
    public static final int SIZE = 256;

    private static final int WORD_SHIFT = 6;
    private static final int BIT_MASK = Long.SIZE - 1;
    private static final long NONE = -1L;


    private final long @NotNull [] accepted;

    private long highest;

    /**
     * Creates a {@link ReplayWindow} instance that has accepted nothing.
     */
    public ReplayWindow() {
        this.accepted = new long[SIZE / Long.SIZE];

        this.highest = NONE;
    }

    /**
     * @return the highest accepted counter, or {@code -1} before the first
     */
    public long getHighest() {
        return this.highest;
    }


    /**
     * Checks a counter without recording it.
     *
     * @param counter the datagram counter
     * @return {@code true} if the counter was not accepted before and is not older than the window, {@code false} otherwise
     */
    public boolean isFresh(long counter) {
        if (counter < 0L) {
            return false;
        }

        if (counter > this.highest) {
            return true;
        }

        return this.highest - counter < SIZE && !this.isAccepted(counter);
    }

    /**
     * Records a fresh counter of an authenticated datagram.
     *
     * @param counter the datagram counter, fresh by {@link #isFresh}
     * @return {@code true} if the counter is the new highest, {@code false} otherwise (it lies below the highest)
     */
    public boolean accept(long counter) {
        boolean newest = counter > this.highest;

        if (newest) {
            if (this.highest == NONE || counter - this.highest >= SIZE) {
                Arrays.fill(this.accepted, 0L);
            } else {
                for (long cleared = this.highest + 1L; cleared < counter; cleared++) {
                    this.setAccepted(cleared, false);
                }
            }

            this.highest = counter;
        }

        this.setAccepted(counter, true);

        return newest;
    }

    private boolean isAccepted(long counter) {
        int index = (int) (counter % SIZE);

        return (this.accepted[index >>> WORD_SHIFT] & 1L << (index & BIT_MASK)) != 0L;
    }

    private void setAccepted(long counter, boolean value) {
        int index = (int) (counter % SIZE);

        if (value) {
            this.accepted[index >>> WORD_SHIFT] |= 1L << (index & BIT_MASK);
        } else {
            this.accepted[index >>> WORD_SHIFT] &= ~(1L << (index & BIT_MASK));
        }
    }
}
