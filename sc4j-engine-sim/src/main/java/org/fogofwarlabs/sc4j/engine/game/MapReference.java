package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Names the map of a game setup: a name for people and a digest of the map contents, so two machines can check that
 * they play the same map. The mod's map loader reads the map itself.
 */
public final class MapReference {
    /**
     * The digest algorithm of {@link #create}.
     */
    public static final @NotNull String DIGEST_ALGORITHM = "SHA-256";

    /**
     * Creates the reference of a map file: its name and the {@value #DIGEST_ALGORITHM} digest of its contents.
     *
     * @param name     the map name
     * @param contents the map file contents
     * @return the created {@link MapReference} instance
     */
    public static @NotNull MapReference create(@NotNull String name, byte @NotNull [] contents) {
        try {
            return new MapReference(name, MessageDigest.getInstance(DIGEST_ALGORITHM).digest(contents));
        } catch (NoSuchAlgorithmException exception) {
            throw Exceptions.newIllegalStateException(exception, "The digest algorithm {0} is not available.", DIGEST_ALGORITHM);
        }
    }


    private final @NotNull String name;
    private final byte @NotNull [] digest;

    /**
     * Creates a {@link MapReference} instance.
     *
     * @param name   the map name
     * @param digest the digest of the map contents, copied; empty for a map without contents
     */
    public MapReference(@NotNull String name, byte @NotNull [] digest) {
        this.name   = name;
        this.digest = digest.clone();
    }

    /**
     * @return the map name
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return a copy of the digest of the map contents
     */
    public byte @NotNull [] getDigest() {
        return this.digest.clone();
    }


    /**
     * Checks whether another reference names the same map: the same name and digest.
     *
     * @param other the other reference
     * @return {@code true} if the name and the digest are equal, {@code false} otherwise
     */
    public boolean isSameMap(@NotNull MapReference other) {
        return this.name.equals(other.name) && Arrays.equals(this.digest, other.digest);
    }
}
