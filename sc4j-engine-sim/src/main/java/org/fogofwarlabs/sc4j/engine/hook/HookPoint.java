package org.fogofwarlabs.sc4j.engine.hook;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies one fixed place in a system where a mod's hooks run, with the interface the hooks implement and how the
 * system combines them. The system that owns the point declares it once, as a constant, and documents in the
 * constant's Javadoc when it calls the point and what the hook arguments mean; points compare by identity.
 *
 * @param <H> the hook interface
 */
public final class HookPoint<H> {
    /**
     * Creates a {@link HookPoint} instance.
     *
     * @param <H>         the hook interface
     * @param name        the point name, for diagnostics
     * @param type        the hook interface
     * @param composition how the calling system combines the hooks
     * @return the created {@link HookPoint} instance
     * @throws IllegalArgumentException if the type is not an interface
     */
    public static <H> @NotNull HookPoint<H> of(@NotNull String name, @NotNull Class<H> type, @NotNull HookComposition composition) {
        if (!type.isInterface()) {
            throw Exceptions.newIllegalArgumentException("The hook type of {0} is not an interface: {1}", name, type.getName());
        }

        return new HookPoint<>(name, type, composition);
    }

    private final @NotNull String name;
    private final @NotNull Class<H> type;
    private final @NotNull HookComposition composition;

    private HookPoint(@NotNull String name, @NotNull Class<H> type, @NotNull HookComposition composition) {
        this.name        = name;
        this.type        = type;
        this.composition = composition;
    }

    /**
     * @return the point name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the hook interface
     */
    public @NotNull Class<H> getType() {
        return this.type;
    }

    /**
     * @return how the calling system combines the hooks
     */
    public @NotNull HookComposition getComposition() {
        return this.composition;
    }


    /**
     * @return the point name and hook interface, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.type.getSimpleName() + ")";
    }
}
