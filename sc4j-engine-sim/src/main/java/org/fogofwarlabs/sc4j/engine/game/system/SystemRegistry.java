package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the system factories of a game definition in registration order.
 * <p>
 * Registering a key that is already listed replaces its factory and keeps its position, so replacing an engine default
 * system does not change the creation and attach order.
 */
public final class SystemRegistry {
    private final @NotNull List<@NotNull SystemKey<?>> keys;
    private final @NotNull List<@NotNull SystemFactory<?>> factories;

    /**
     * Creates an empty {@link SystemRegistry} instance.
     */
    public SystemRegistry() {
        this.keys      = new ArrayList<>();
        this.factories = new ArrayList<>();
    }

    /**
     * Creates a {@link SystemRegistry} instance with the registrations of another registry.
     *
     * @param other the registry to copy
     */
    public SystemRegistry(@NotNull SystemRegistry other) {
        this.keys      = new ArrayList<>(other.keys);
        this.factories = new ArrayList<>(other.factories);
    }

    /**
     * @return the number of registered systems
     */
    public int getCount() {
        return this.keys.size();
    }

    /**
     * Returns the key of one registered system.
     *
     * @param index the position in registration order
     * @return the key
     */
    public @NotNull SystemKey<?> getKey(int index) {
        return this.keys.get(index);
    }

    /**
     * Checks whether a key is registered.
     *
     * @param key the key
     * @return {@code true} if the key is registered, {@code false} otherwise
     */
    public boolean contains(@NotNull SystemKey<?> key) {
        return this.findIndex(key) >= 0;
    }


    /**
     * Registers the factory of a system: appends the key, or replaces the factory of a registered key at its position.
     *
     * @param <S>     the system type
     * @param key     the key
     * @param factory creates the system
     */
    public <S extends GameSystem> void use(@NotNull SystemKey<S> key, @NotNull SystemFactory<? extends S> factory) {
        int index = this.findIndex(key);

        if (index < 0) {
            this.keys.add(key);
            this.factories.add(factory);
        } else {
            this.factories.set(index, factory);
        }
    }

    /**
     * Creates every registered system for a new game, in registration order; each factory sees the systems created before it. The systems
     * are not attached yet.
     *
     * @param setup the game setup
     * @return the created systems
     * @throws IllegalStateException if a factory creates a system that is not of its key's type
     */
    public @NotNull SystemSet createSystems(@NotNull GameSetup setup) {
        SystemKey<?>[] createdKeys = this.keys.toArray(new SystemKey<?>[0]);
        GameSystem[] systems = new GameSystem[createdKeys.length];
        SystemCreationContext context = new SystemCreationContext(setup, createdKeys, systems, 0);

        for (int index = 0; index < createdKeys.length; index++) {
            GameSystem system = this.factories.get(index).create(context);

            if (!createdKeys[index].getType().isInstance(system)) {
                throw Exceptions.newIllegalStateException("The factory of {0} created a {1}.", createdKeys[index], system.getClass().getName());
            }

            systems[index] = system;
            context.setCreatedCount(index + 1);
        }

        return new SystemSet(createdKeys, systems);
    }

    private int findIndex(@NotNull SystemKey<?> key) {
        for (int index = 0; index < this.keys.size(); index++) {
            if (this.keys.get(index) == key) {
                return index;
            }
        }

        return -1;
    }
}
