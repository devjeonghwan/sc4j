package org.fogofwarlabs.sc4j.launcher.session;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

/**
 * Holds a player's friend code: the player's key in Base32 (RFC 4648 alphabet, upper case) without padding, {@value #LENGTH} characters,
 * shown in groups of {@value #GROUP_LENGTH}. A player gives the code to a friend, who adds the player by it; the player's name and tag cannot
 * be used for that, since the tag is a hash of the key. A code never changes.
 */
public final class FriendCode {
    /**
     * The length of a player's key in bytes.
     */
    public static final int KEY_LENGTH = 32;
    /**
     * The number of characters of a code.
     */
    public static final int LENGTH = 52;
    /**
     * The number of characters of a group in the grouped text.
     */
    public static final int GROUP_LENGTH = 4;

    private static final @NotNull String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int BITS_PER_CHARACTER = 5;
    private static final int BITS_PER_BYTE = 8;
    private static final int CHARACTER_MASK = 0x1F;
    private static final int BYTE_MASK = 0xFF;
    private static final char GROUP_SEPARATOR = ' ';
    private static final char HYPHEN = '-';

    /**
     * Creates a {@link FriendCode} instance of a player's key.
     *
     * @param key the player's key, {@value #KEY_LENGTH} bytes
     * @return the created {@link FriendCode} instance
     * @throws IllegalArgumentException if the key is not {@value #KEY_LENGTH} bytes long
     */
    public static @NotNull FriendCode of(byte @NotNull [] key) {
        if (key.length != KEY_LENGTH) {
            throw Exceptions.newIllegalArgumentException("A player key has {0} bytes, not {1}.", KEY_LENGTH, key.length);
        }

        StringBuilder text = new StringBuilder(LENGTH);
        int buffer = 0;
        int bits = 0;

        for (byte value : key) {
            buffer = (buffer << BITS_PER_BYTE) | (value & BYTE_MASK);
            bits += BITS_PER_BYTE;

            while (bits >= BITS_PER_CHARACTER) {
                bits -= BITS_PER_CHARACTER;
                text.append(ALPHABET.charAt((buffer >>> bits) & CHARACTER_MASK));
            }
        }

        if (bits > 0) {
            text.append(ALPHABET.charAt((buffer << (BITS_PER_CHARACTER - bits)) & CHARACTER_MASK));
        }

        return new FriendCode(key.clone(), text.toString());
    }

    /**
     * Creates a {@link FriendCode} instance from the text a user entered: the code in any case, with or without spaces and hyphens between its
     * characters.
     *
     * @param text the entered text
     * @return the created {@link FriendCode} instance
     * @throws IllegalArgumentException if the text is not a friend code
     */
    public static @NotNull FriendCode parse(@NotNull String text) {
        StringBuilder compact = new StringBuilder(LENGTH);

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (!Character.isWhitespace(character) && character != HYPHEN) {
                compact.append(character);
            }
        }

        String code = compact.toString().toUpperCase(Locale.ROOT);

        if (code.length() != LENGTH) {
            throw Exceptions.newIllegalArgumentException("A friend code has {0} characters, not {1}.", LENGTH, code.length());
        }

        byte[] key = new byte[KEY_LENGTH];
        int buffer = 0;
        int bits = 0;
        int written = 0;

        for (int index = 0; index < code.length(); index++) {
            int value = ALPHABET.indexOf(code.charAt(index));

            if (value < 0) {
                throw Exceptions.newIllegalArgumentException("A friend code has no character {0}.", code.charAt(index));
            }

            buffer = (buffer << BITS_PER_CHARACTER) | value;
            bits += BITS_PER_CHARACTER;

            if (bits >= BITS_PER_BYTE) {
                bits -= BITS_PER_BYTE;
                key[written++] = (byte) (buffer >>> bits);
            }
        }

        // The bits after the last key byte pad the last character; a code written from a key leaves them zero.
        if ((buffer & ((1 << bits) - 1)) != 0) {
            throw Exceptions.newIllegalArgumentException("The friend code {0} does not end as a written code does.", code);
        }

        return new FriendCode(key, code);
    }


    private final byte @NotNull [] key;
    private final @NotNull String text;

    private FriendCode(byte @NotNull [] key, @NotNull String text) {
        this.key  = key;
        this.text = text;
    }

    /**
     * @return a copy of the player's key
     */
    public byte @NotNull [] getKey() {
        return this.key.clone();
    }

    /**
     * @return the code, {@value #LENGTH} characters without separators
     */
    public @NotNull String getText() {
        return this.text;
    }

    /**
     * @return the code in groups of {@value #GROUP_LENGTH} characters separated by spaces, as the launcher shows it
     */
    public @NotNull String getGroupedText() {
        StringBuilder grouped = new StringBuilder(LENGTH + LENGTH / GROUP_LENGTH);

        for (int index = 0; index < LENGTH; index += GROUP_LENGTH) {
            if (index > 0) {
                grouped.append(GROUP_SEPARATOR);
            }

            grouped.append(this.text, index, index + GROUP_LENGTH);
        }

        return grouped.toString();
    }


    /**
     * Indicates whether some other object is "equal to" this one: another code of the same key.
     *
     * @param other the reference object with which to compare
     * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof FriendCode code && Arrays.equals(this.key, code.key);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.key);
    }

    /**
     * Returns a string representation of the object.
     *
     * @return the code without separators
     */
    @Override
    public @NotNull String toString() {
        return this.text;
    }
}
