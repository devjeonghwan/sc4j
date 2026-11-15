package org.fogofwarlabs.sc4j.engine.game.system;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies one game system by the type the other systems use. The engine and the mods declare each key once, as a
 * constant; keys compare by identity.
 *
 * @param <S> the system type
 */
public final class SystemKey<S extends GameSystem> {
    /**
     * Creates a {@link SystemKey} instance.
     *
     * @param <S>  the system type
     * @param name the system name, for diagnostics
     * @param type the system type
     * @return the created {@link SystemKey} instance
     */
    public static <S extends GameSystem> @NotNull SystemKey<S> of(@NotNull String name, @NotNull Class<S> type) {
        return new SystemKey<>(name, type);
    }

    private final @NotNull String name;
    private final @NotNull Class<S> type;

    private SystemKey(@NotNull String name, @NotNull Class<S> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * @return the system name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the system type
     */
    public @NotNull Class<S> getType() {
        return this.type;
    }


    /**
     * @return the system name and type, for diagnostics
     */
    @Override
    public @NotNull String toString() {
        return this.name + " (" + this.type.getSimpleName() + ")";
    }
}
