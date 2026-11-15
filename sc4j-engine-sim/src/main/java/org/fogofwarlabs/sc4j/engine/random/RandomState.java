package org.fogofwarlabs.sc4j.engine.random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Holds the saved state of a {@link DeterministicRandom} as a sequence of {@code 32}-bit words. The implementation that
 * saved it defines the meaning of the words; the words can be written to a save game or a replay as they are.
 */
public final class RandomState {
    /**
     * Creates a {@link RandomState} instance from saved words.
     *
     * @param words the words, copied
     * @return the created {@link RandomState} instance
     */
    public static @NotNull RandomState of(int @NotNull ... words) {
        return new RandomState(words.clone());
    }

    private final int @NotNull [] words;

    private RandomState(int @NotNull [] words) {
        this.words = words;
    }

    /**
     * @return the number of words
     */
    public int getWordCount() {
        return this.words.length;
    }

    /**
     * Returns one word.
     *
     * @param index the word index, {@code 0} up to {@link #getWordCount()} minus one
     * @return the word
     */
    public int getWord(int index) {
        return this.words[index];
    }

    /**
     * @return a copy of every word
     */
    public int @NotNull [] toArray() {
        return this.words.clone();
    }

    /**
     * Compares the words of two states.
     *
     * @param other the other object
     * @return {@code true} if the other object is a {@link RandomState} with the same words, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof RandomState state && Arrays.equals(this.words, state.words);
    }

    /**
     * @return the hash code of the words
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.words);
    }

    /**
     * @return the words in hexadecimal, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        StringBuilder text = new StringBuilder("RandomState[");

        for (int index = 0; index < this.words.length; index++) {
            if (index > 0) {
                text.append(", ");
            }

            text.append(Integer.toHexString(this.words[index]));
        }

        return text.append(']').toString();
    }
}
