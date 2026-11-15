package org.fogofwarlabs.sc4j.engine.resource;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Names one resource of a provider, such as {@code units/footman} or {@code text/en}.
 * <p>
 * A key is one or more segments separated by {@code /}. A segment holds ASCII letters, digits, {@code _}, {@code -} and
 * {@code .}, and is neither {@code .} nor {@code ..}. Keys compare by their name, case-sensitively; what a key names is
 * up to the provider.
 */
public final class ResourceKey {
    /**
     * The separator of the key segments.
     */
    public static final char SEGMENT_SEPARATOR = '/';

    /**
     * The segment that names the current folder, which a key may not hold.
     */
    private static final @NotNull String CURRENT_SEGMENT = ".";
    /**
     * The segment that names the parent folder, which a key may not hold.
     */
    private static final @NotNull String PARENT_SEGMENT = "..";

    /**
     * Creates a {@link ResourceKey} instance.
     *
     * @param name the key name
     * @return the created {@link ResourceKey} instance
     * @throws IllegalArgumentException if the name is not a valid key
     */
    public static @NotNull ResourceKey of(@NotNull String name) {
        int segmentStart = 0;

        for (int index = 0; index <= name.length(); index++) {
            if (index == name.length() || name.charAt(index) == SEGMENT_SEPARATOR) {
                String segment = name.substring(segmentStart, index);

                if (segment.isEmpty() || segment.equals(CURRENT_SEGMENT) || segment.equals(PARENT_SEGMENT)) {
                    throw Exceptions.newIllegalArgumentException("Invalid resource key segment {0} in: {1}", segment, name);
                }

                segmentStart = index + 1;
            } else if (!isKeyCharacter(name.charAt(index))) {
                throw Exceptions.newIllegalArgumentException("Invalid resource key character at {0} in: {1}", index, name);
            }
        }

        return new ResourceKey(name);
    }

    private static boolean isKeyCharacter(char character) {
        return character >= 'a' && character <= 'z' ||
               character >= 'A' && character <= 'Z' ||
               character >= '0' && character <= '9' ||
               character == '_' || character == '-' || character == '.';
    }


    private final @NotNull String name;

    private ResourceKey(@NotNull String name) {
        this.name = name;
    }

    /**
     * @return the key name, segments separated by {@link #SEGMENT_SEPARATOR}
     */
    public @NotNull String getName() {
        return this.name;
    }


    /**
     * Checks whether another object is a key with the same name.
     *
     * @param other the other object
     * @return {@code true} if the other object is a key with the same name, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ResourceKey key && this.name.equals(key.name);
    }

    /**
     * @return the hash code of the name
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * @return the key name
     */
    @Override
    public @NotNull String toString() {
        return this.name;
    }
}
