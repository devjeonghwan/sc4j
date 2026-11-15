package org.fogofwarlabs.sc4j.engine.command;

import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Defines one kind of command, the only input to the simulation: its id, the codec of its payload and what it does to
 * the game. The mod owns the codec: the bytes {@link #write} produces are what replays, the network and recorders store,
 * and {@link #read} reads them back into an equal command.
 * <p>
 * A command type is immutable; one instance serves every command of its kind. A mod registers its types in a
 * {@link CommandRegistry} by id.
 * <p>
 * Determinism contract: {@link #execute} runs at the command point of a frame ({@link
 * org.fogofwarlabs.sc4j.engine.game.frame.CommandSource}), in the recorded order of the commands, and changes the game
 * only through its systems, its units and the random generator; what it does depends only on the game, the issuing
 * player and the command.
 *
 * @param <C> the command class: a decoded payload
 */
public interface CommandType<C> {
    /**
     * @return the id of the type in the mod's command stream, {@code 0} or more
     */
    int getId();

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @NotNull C read(@NotNull CommandInput input) throws IOException;

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    void write(@NotNull C command, @NotNull CommandOutput output);

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    void execute(@NotNull Game game, @NotNull Player player, @NotNull C command);
}
