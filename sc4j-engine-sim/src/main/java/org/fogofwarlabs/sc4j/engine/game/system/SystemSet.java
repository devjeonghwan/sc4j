package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the systems of one game in registration order.
 * <p>
 * Lookups by key search the registration order; they are meant for {@link GameSystem#attach} and game creation, not
 * for the frame.
 */
public final class SystemSet {
    private final @NotNull SystemKey<?> @NotNull [] keys;
    private final @NotNull GameSystem @NotNull [] systems;

    private boolean attached;

    /**
     * Creates a {@link SystemSet} instance of created, not yet attached systems.
     *
     * @param keys    the keys in registration order
     * @param systems the systems, one per key
     */
    SystemSet(@NotNull SystemKey<?> @NotNull [] keys, @NotNull GameSystem @NotNull [] systems) {
        this.keys    = keys;
        this.systems = systems;

        this.attached = false;
    }

    /**
     * @return the number of systems
     */
    public int getCount() {
        return this.systems.length;
    }

    /**
     * Returns the key of one system.
     *
     * @param index the position in registration order
     * @return the key
     */
    public @NotNull SystemKey<?> getKey(int index) {
        return this.keys[index];
    }

    /**
     * Returns one system.
     *
     * @param index the position in registration order
     * @return the system
     */
    public @NotNull GameSystem get(int index) {
        return this.systems[index];
    }

    /**
     * Returns the system of a key.
     *
     * @param <S> the system type
     * @param key the key
     * @return the system
     * @throws IllegalArgumentException if the key is not registered
     */
    public <S extends GameSystem> @NotNull S get(@NotNull SystemKey<S> key) {
        S system = this.find(key);

        if (system == null) {
            throw Exceptions.newIllegalArgumentException("The system {0} is not registered.", key);
        }

        return system;
    }

    /**
     * Finds the system of a key.
     *
     * @param <S> the system type
     * @param key the key
     * @return the system, or {@code null} if the key is not registered
     */
    public <S extends GameSystem> @Nullable S find(@NotNull SystemKey<S> key) {
        for (int index = 0; index < this.keys.length; index++) {
            if (this.keys[index] == key) {
                return key.getType().cast(this.systems[index]);
            }
        }

        return null;
    }

    /**
     * @return {@code true} if {@link #attach} ran, {@code false} otherwise
     */
    public boolean isAttached() {
        return this.attached;
    }


    /**
     * Creates the context of the hook factories of the game: its setup and every system of this set.
     *
     * @param setup the setup of the game
     * @return the created context
     */
    public @NotNull SystemContext createContext(@NotNull GameSetup setup) {
        return new SystemCreationContext(setup, this.keys, this.systems, this.systems.length);
    }

    /**
     * Attaches every system to its game, in registration order.
     *
     * @param game the game the systems belong to
     * @throws IllegalStateException if the systems are already attached
     */
    public void attach(@NotNull Game game) {
        if (this.attached) {
            throw Exceptions.newIllegalStateException("The systems are already attached.");
        }

        this.attached = true;

        for (GameSystem system : this.systems) {
            system.attach(game);
        }
    }
}
