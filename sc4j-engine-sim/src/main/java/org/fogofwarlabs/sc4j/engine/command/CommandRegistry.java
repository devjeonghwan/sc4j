package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the {@link CommandType} of every command id of one command stream: one slot per id, {@code 0} up to the id count
 * minus one. The dispatcher of the stream finds the type of an id here, reads the command and executes it
 * ({@link #readAndExecute}); an id without a type is not a command of the stream.
 * <p>
 * The mod fills a registry before the first frame and then {@link #freeze freezes} it. A lookup is an array read and
 * allocates nothing.
 *
 * @param <T> the command type class of the stream
 */
public final class CommandRegistry<T extends CommandType<?>> {
    /**
     * Reads a command of a type and executes it.
     *
     * @param <C>    the command class
     * @param type   the command type
     * @param game   the game
     * @param player the issuing player
     * @param input  the payload of the command
     * @throws IOException if the payload is not a command of the type
     */
    public static <C> void readAndExecute(@NotNull CommandType<C> type, @NotNull Game game, @NotNull Player player, @NotNull CommandInput input) throws IOException {
        type.execute(game, player, type.read(input));
    }


    private final @Nullable Object @NotNull [] types;
    private boolean frozen;

    /**
     * Creates an empty {@link CommandRegistry} instance.
     *
     * @param idCount the number of ids, {@code 0} to {@code idCount - 1}
     * @throws IllegalArgumentException if the count is negative
     */
    public CommandRegistry(int idCount) {
        if (idCount < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid command id count: {0}", idCount);
        }

        this.types  = new Object[idCount];
        this.frozen = false;
    }

    /**
     * @return the number of ids
     */
    public int getIdCount() {
        return this.types.length;
    }

    /**
     * @return {@code true} if the registration has ended, {@code false} otherwise
     */
    public boolean isFrozen() {
        return this.frozen;
    }

    /**
     * Returns the type of an id.
     *
     * @param id the command id
     * @return the type
     * @throws IllegalArgumentException if the id has no type
     */
    public @NotNull T get(int id) {
        T type = this.find(id);

        if (type == null) {
            throw Exceptions.newIllegalArgumentException("The command id {0} has no command type.", id);
        }

        return type;
    }

    /**
     * Finds the type of an id.
     *
     * @param id the command id, any value
     * @return the type, or {@code null} if the id has none or lies outside the registry
     */
    public @Nullable T find(int id) {
        return id >= 0 && id < this.types.length ? this.cast(this.types[id]) : null;
    }

    /**
     * Registers a type at its id.
     *
     * @param type the type
     * @throws IllegalArgumentException if its id lies outside the registry or already has a type
     * @throws IllegalStateException    if the registry is frozen
     */
    public void register(@NotNull T type) {
        int id = type.getId();

        if (this.frozen) {
            throw Exceptions.newIllegalStateException("The command registry is frozen; the command id {0} cannot be registered.", id);
        }

        if (id < 0 || id >= this.types.length) {
            throw Exceptions.newIllegalArgumentException("The command id {0} lies outside the {1} ids of the registry.", id, this.types.length);
        }

        if (this.types[id] != null) {
            throw Exceptions.newIllegalArgumentException("The command id {0} already has a command type.", id);
        }

        this.types[id] = type;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.frozen = true;
    }

    @SuppressWarnings("unchecked")
    private @Nullable T cast(@Nullable Object type) {
        return (T) type;
    }
}
