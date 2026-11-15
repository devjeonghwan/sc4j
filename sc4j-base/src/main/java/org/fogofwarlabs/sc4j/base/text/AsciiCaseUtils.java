package org.fogofwarlabs.sc4j.base.text;

import org.jetbrains.annotations.NotNull;

/**
 * Folds and compares names with the ASCII-only case rule: {@code A}-{@code Z} match {@code a}-{@code z}, and every other
 * character, a non-ASCII letter too, matches only itself.
 * <p>
 * Player, game, saved game, map file and multicast DNS names follow this rule on their UTF-8 bytes. A UTF-8 multi-byte
 * sequence holds no ASCII byte, so folding the characters of a {@link String} gives the same result.
 */
public final class AsciiCaseUtils {
    private static final int FIRST_UPPER_CASE = 'A';
    private static final int LAST_UPPER_CASE = 'Z';
    private static final int CASE_OFFSET = 'a' - 'A';

    /**
     * Folds one character or byte value to lower case if it is an ASCII upper-case letter.
     *
     * @param character a character or an unsigned byte value
     * @return the lower-case letter for {@code A}-{@code Z}, else the argument
     */
    public static int toLowerCase(int character) {
        return character >= FIRST_UPPER_CASE && character <= LAST_UPPER_CASE ? character + CASE_OFFSET : character;
    }

    /**
     * Folds the ASCII upper-case letters of a text to lower case; a map key of a name uses this form.
     *
     * @param text the text
     * @return the folded text, the argument itself when it has no ASCII upper-case letter
     */
    public static @NotNull String toLowerCase(@NotNull String text) {
        char[] characters = null;

        for (int index = 0; index < text.length(); index++) {
            int character = text.charAt(index);
            int folded = toLowerCase(character);

            if (folded != character) {
                if (characters == null) {
                    characters = text.toCharArray();
                }

                characters[index] = (char) folded;
            }
        }

        return characters == null ? text : new String(characters);
    }

    /**
     * Compares two texts with the ASCII-only case rule.
     *
     * @param first  a text
     * @param second another text
     * @return {@code true} if the texts are equal after folding their ASCII letters, {@code false} otherwise
     */
    public static boolean equalsIgnoreCase(@NotNull String first, @NotNull String second) {
        if (first.length() != second.length()) {
            return false;
        }

        for (int index = 0; index < first.length(); index++) {
            if (toLowerCase(first.charAt(index)) != toLowerCase(second.charAt(index))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares the bytes of two texts with the ASCII-only case rule.
     *
     * @param first  the bytes of a text
     * @param second the bytes of another text
     * @return {@code true} if the bytes are equal after folding their ASCII letters, {@code false} otherwise
     */
    public static boolean equalsIgnoreCase(byte @NotNull [] first, byte @NotNull [] second) {
        if (first.length != second.length) {
            return false;
        }

        for (int index = 0; index < first.length; index++) {
            if (toLowerCase(Byte.toUnsignedInt(first[index])) != toLowerCase(Byte.toUnsignedInt(second[index]))) {
                return false;
            }
        }

        return true;
    }

    private AsciiCaseUtils() {
    }
}
