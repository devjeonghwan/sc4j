package org.fogofwarlabs.sc4j.engine.resource;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serial;

/**
 * Signals that a provider has no resource of a type under a key. A resource that exists but cannot be read is reported
 * by a plain {@link IOException} instead, so a caller can tell an optional resource that is absent from a broken one.
 */
public final class ResourceNotFoundException extends IOException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link ResourceNotFoundException} instance for a missing resource.
     *
     * @param type the resource type
     * @param key  the key, or {@code null} for a resource without key, such as the animation library
     * @return the created {@link ResourceNotFoundException} instance
     */
    public static @NotNull ResourceNotFoundException create(@NotNull ResourceType type, @Nullable ResourceKey key) {
        return new ResourceNotFoundException(type, key, Exceptions.format("No resource of type {0} under the key {1}.", type, key));
    }


    private final @NotNull ResourceType type;
    private final @Nullable String keyName;

    /**
     * Creates a {@link ResourceNotFoundException} instance.
     *
     * @param type    the resource type
     * @param key     the key, or {@code null}
     * @param message the formatted message
     */
    private ResourceNotFoundException(@NotNull ResourceType type, @Nullable ResourceKey key, @NotNull String message) {
        super(message);

        this.type    = type;
        this.keyName = key != null ? key.getName() : null;
    }

    /**
     * @return the type of the missing resource
     */
    public @NotNull ResourceType getType() {
        return this.type;
    }

    /**
     * @return the key of the missing resource, or {@code null} for a resource without key
     */
    public @Nullable ResourceKey getKey() {
        String name = this.keyName;

        return name != null ? ResourceKey.of(name) : null;
    }
}
