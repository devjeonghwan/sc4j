package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Holds the rules of player names, the one place that defines them: the launcher's new account, the account store and every peer that checks
 * a registration or a record use them. A name is the normalized form of what the user typed: Unicode NFC, without white space around it and
 * with every run of white space inside it made one space. A valid name is 1 to {@value #MAXIMUM_LENGTH} UTF-8 bytes long, holds no tag
 * separator {@value #TAG_SEPARATOR} and no control, format, private-use, surrogate or unassigned character, and is its own normalized form.
 * The player stamp binds the name to the player key ({@link PlayerStamp}), so that an account's name never changes.
 */
public final class PlayerNameUtils {
    /**
     * The largest UTF-8 length of a name.
     */
    public static final int MAXIMUM_LENGTH = 32;
    /**
     * The separator between a name and its tag ({@code Name#abcd12}), which a name cannot hold.
     */
    public static final char TAG_SEPARATOR = '#';

    private static final char SPACE = ' ';

    /**
     * Normalizes a typed name.
     *
     * @param typed the name as typed
     * @return the normalized name, or {@code null} if it is not a valid name
     */
    public static @Nullable String normalize(@NotNull String typed) {
        String composed = Normalizer.normalize(typed, Normalizer.Form.NFC).strip();
        StringBuilder name = new StringBuilder(composed.length());
        boolean space = false;

        for (int index = 0; index < composed.length(); ) {
            int codePoint = composed.codePointAt(index);

            index += Character.charCount(codePoint);

            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                space = true;

                continue;
            }

            if (codePoint == TAG_SEPARATOR || !isAllowed(codePoint)) {
                return null;
            }

            if (space) {
                name.append(SPACE);
                space = false;
            }

            name.appendCodePoint(codePoint);
        }

        int length = name.toString().getBytes(StandardCharsets.UTF_8).length;

        return length == 0 || length > MAXIMUM_LENGTH ? null : name.toString();
    }

    /**
     * Checks a name.
     *
     * @param name the name
     * @return {@code true} if the name is valid and normalized, {@code false} otherwise
     */
    public static boolean isValid(@NotNull String name) {
        return name.equals(normalize(name));
    }

    private static boolean isAllowed(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONTROL, Character.FORMAT, Character.PRIVATE_USE, Character.SURROGATE, Character.UNASSIGNED -> false;
            default -> true;
        };
    }


    private PlayerNameUtils() {
    }
}
