package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Replaces one name in interface text by another, for example a product or service name, keeping the way it is written.
 * <ul>
 * <li>A name written in mixed case (for example {@code Example.net} or {@code Example.Net}) becomes the replacement as given, one written all in
 * upper case becomes the replacement in upper case; the rest of the string, such as a following symbol or particle, is kept.</li>
 * <li>A name written all in lower case is kept (a domain name or identifier), as is a name that is part of a longer word or host name: a
 * letter, digit, dot or slash right before it, or a letter or digit right after it.</li>
 * </ul>
 * The name is matched case-insensitively.
 */
public final class NameSubstitution implements TextSubstitutionHook {
    private static final int BUILDER_CAPACITY_MARGIN = 4;

    private static boolean isAsciiLetterOrDigit(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z' || character >= '0' && character <= '9';
    }


    private final @NotNull String sourceName;
    private final @NotNull String replacementName;

    /**
     * Creates a {@link NameSubstitution} instance.
     *
     * @param sourceName      the name to replace, in any case
     * @param replacementName the name shown instead, as written in mixed case
     * @throws IllegalArgumentException if the source name is empty
     */
    public NameSubstitution(@NotNull String sourceName, @NotNull String replacementName) {
        if (sourceName.isEmpty()) {
            throw Exceptions.newIllegalArgumentException("The source name of a name substitution is empty.");
        }

        this.sourceName      = sourceName.toLowerCase(Locale.ROOT);
        this.replacementName = replacementName;
    }

    /**
     * Rewrites one interface string.
     *
     * @param text the interface text
     * @return the rewritten text, the same instance when nothing changes
     */
    @Override
    public @NotNull String substitute(@NotNull String text) {
        int nameLength = this.sourceName.length();
        int found = this.findNameIndex(text, 0);

        if (found < 0) {
            return text;
        }

        StringBuilder builder = new StringBuilder(text.length() + BUILDER_CAPACITY_MARGIN);
        int copied = 0;

        for (; found >= 0; found = this.findNameIndex(text, found + nameLength)) {
            String name = text.substring(found, found + nameLength);
            boolean upper = name.equals(name.toUpperCase(Locale.ROOT));
            boolean lower = name.equals(name.toLowerCase(Locale.ROOT));

            if (lower) {
                continue;
            }

            if (found > 0) {
                char previousCharacter = text.charAt(found - 1);

                if (isAsciiLetterOrDigit(previousCharacter) || previousCharacter == '.' || previousCharacter == '/') {
                    continue;
                }
            }

            int nameEnd = found + nameLength;

            if (nameEnd < text.length() && isAsciiLetterOrDigit(text.charAt(nameEnd))) {
                continue;
            }

            builder.append(text, copied, found).append(upper ? this.replacementName.toUpperCase(Locale.ROOT) : this.replacementName);

            copied = found + nameLength;
        }

        return copied == 0 ? text : builder.append(text, copied, text.length()).toString();
    }

    private int findNameIndex(@NotNull String text, int startIndex) {
        int nameLength = this.sourceName.length();

        for (int index = startIndex; index + nameLength <= text.length(); index++) {
            if (text.regionMatches(true, index, this.sourceName, 0, nameLength)) {
                return index;
            }
        }

        return -1;
    }
}
