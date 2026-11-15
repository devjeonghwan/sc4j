package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the condition or the action of every type id of one trigger format: one slot per id, {@code 0} up to the type count minus one. The
 * type id is the key a mod's trigger records store, as the command id is the key of a command stream, so the registry is indexed by it; a
 * trigger system finds the entry of a record's type here.
 * <p>
 * The mod fills a registry before the first frame and then {@link #freeze freezes} it; registering again before the freeze replaces the
 * entry of the type. What a type at or above {@link #getCount()} means is the trigger system's rule (for example: such a condition does not
 * hold, such an action does nothing). A lookup is an array read and allocates nothing.
 *
 * @param <B> the condition or action type of the registry
 */
public final class TriggerRegistry<B> {
    private final @Nullable Object @NotNull [] entries;
    private boolean frozen;

    /**
     * Creates an empty {@link TriggerRegistry} instance.
     *
     * @param typeCount the number of type ids, {@code 0} to {@code typeCount - 1}
     * @throws IllegalArgumentException if the count is negative
     */
    public TriggerRegistry(int typeCount) {
        if (typeCount < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid trigger type count: {0}", typeCount);
        }

        this.entries = new Object[typeCount];
        this.frozen  = false;
    }

    /**
     * @return the number of type ids
     */
    public int getCount() {
        return this.entries.length;
    }

    /**
     * @return {@code true} if the registration has ended, {@code false} otherwise
     */
    public boolean isFrozen() {
        return this.frozen;
    }

    /**
     * Finds the entry of a type id.
     *
     * @param type the type id, any value
     * @return the entry, or {@code null} if the type has none or lies outside the registry
     */
    public @Nullable B find(int type) {
        return type >= 0 && type < this.entries.length ? this.cast(this.entries[type]) : null;
    }

    /**
     * Registers the entry of a type id, replacing a previous one.
     *
     * @param type  the type id
     * @param entry the condition or action of the type
     * @throws IllegalArgumentException if the type lies outside the registry
     * @throws IllegalStateException    if the registry is frozen
     */
    public void register(int type, @NotNull B entry) {
        if (this.frozen) {
            throw Exceptions.newIllegalStateException("The trigger registry is frozen; the type {0} cannot be registered.", type);
        }

        if (type < 0 || type >= this.entries.length) {
            throw Exceptions.newIllegalArgumentException("Trigger type outside the table of {0}: {1}", this.entries.length, type);
        }

        this.entries[type] = entry;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.frozen = true;
    }

    @SuppressWarnings("unchecked")
    private @Nullable B cast(@Nullable Object entry) {
        return (B) entry;
    }
}
