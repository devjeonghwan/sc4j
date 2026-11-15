package org.fogofwarlabs.sc4j.manifest.version;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an engine API version {@code major.minor}: the version a mod is built against (the manifest field
 * {@code engine}) or the version a runtime provides ({@link #CURRENT}).
 * <p>
 * A runtime supports a mod when both have the same major version and the mod requires no higher minor version than
 * the runtime provides ({@link #supports(EngineVersion)}).
 */
public final class EngineVersion {
    /**
     * The engine API version of this SC4J release. The launcher and the runtime check mods against it.
     */
    public static final @NotNull EngineVersion CURRENT = new EngineVersion(0, 1);

    private static final @NotNull Pattern VERSION_PATTERN = Pattern.compile("(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})");
    private static final int MAJOR_GROUP = 1;
    private static final int MINOR_GROUP = 2;
    private static final int HASH_MULTIPLIER = 31;

    /**
     * Creates a {@link EngineVersion} instance from its text form.
     *
     * @param text the version text, such as {@code 1.0}
     * @return the created {@link EngineVersion} instance
     * @throws IllegalArgumentException if the text is not {@code major.minor} with decimal numbers
     */
    public static @NotNull EngineVersion parse(@NotNull String text) {
        Matcher matcher = VERSION_PATTERN.matcher(text);

        if (!matcher.matches()) {
            throw Exceptions.newIllegalArgumentException("Invalid engine version (expected major.minor): {0}", text);
        }

        return new EngineVersion(Integer.parseInt(matcher.group(MAJOR_GROUP)), Integer.parseInt(matcher.group(MINOR_GROUP)));
    }


    private final int major;
    private final int minor;

    /**
     * Creates a {@link EngineVersion} instance.
     *
     * @param major the major version; a change breaks mods built for another major version
     * @param minor the minor version; a change only adds to the API
     */
    private EngineVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * @return the major version
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * @return the minor version
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Checks whether a runtime of this version can run a mod built for a version.
     *
     * @param required the engine version the mod is built for
     * @return {@code true} if the major versions are equal and the required minor version is not higher than this one,
     * {@code false} otherwise
     */
    public boolean supports(@NotNull EngineVersion required) {
        return this.major == required.major && required.minor <= this.minor;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation on non-null object references.
     *
     * @param other the reference object with which to compare
     * @return {@code true} if this object is the same as the other argument, {@code false} otherwise
     * @implSpec Two engine versions are equal when their major and minor versions are equal.
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof EngineVersion version && this.major == version.major && this.minor == version.minor;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     * @implSpec Combines the major and minor versions, consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return this.major * HASH_MULTIPLIER + this.minor;
    }

    /**
     * {@return a string representation of the object}
     *
     * @implSpec Returns the manifest text form {@code major.minor}.
     */
    @Override
    public @NotNull String toString() {
        return this.major + "." + this.minor;
    }
}
