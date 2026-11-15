package org.fogofwarlabs.sc4j.engine.game.system;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.jetbrains.annotations.NotNull;

/**
 * Gives the factories of a new game the systems created so far: the first entries of the arrays, up to the created count.
 */
final class SystemCreationContext implements SystemContext {
    private final @NotNull GameSetup setup;
    private final @NotNull SystemKey<?> @NotNull [] keys;
    private final @NotNull GameSystem @NotNull [] systems;

    private int createdCount;

    /**
     * Creates a {@link SystemCreationContext} instance.
     *
     * @param setup        the setup of the new game
     * @param keys         the keys in registration order
     * @param systems      the systems in registration order, filled up to the created count
     * @param createdCount the number of systems created so far
     */
    SystemCreationContext(@NotNull GameSetup setup,
                          @NotNull SystemKey<?> @NotNull [] keys, @NotNull GameSystem @NotNull [] systems,
                          int createdCount) {
        this.setup   = setup;
        this.keys    = keys;
        this.systems = systems;

        this.createdCount = createdCount;
    }

    /**
     * @return the setup of the new game
     */
    @Override
    public @NotNull GameSetup getSetup() {
        return this.setup;
    }

    /**
     * Sets the number of systems created so far, after the next system is stored.
     *
     * @param createdCount the number
     */
    void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }


    /**
     * Returns a system the game created before: for a system factory one registered before its own key, for a hook factory any system.
     *
     * @param <S> the system type
     * @param key the key
     * @return the created system, not attached yet
     * @throws IllegalStateException if the system of the key is not created yet or not registered
     */
    @Override
    public <S extends GameSystem> @NotNull S require(@NotNull SystemKey<S> key) {
        for (int index = 0; index < this.createdCount; index++) {
            if (this.keys[index] == key) {
                return key.getType().cast(this.systems[index]);
            }
        }

        throw Exceptions.newIllegalStateException("The system {0} is not created yet; a factory can use only the systems registered before it.", key);
    }
}
