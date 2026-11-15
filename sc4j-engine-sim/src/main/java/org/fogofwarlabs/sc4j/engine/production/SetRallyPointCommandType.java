package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Sets the rally point of a player's producing unit: the engine default command of a person's rally point. A mod registers it in its
 * {@link org.fogofwarlabs.sc4j.engine.command.CommandRegistry} under the id of its command stream.
 * {@snippet :
 * SetRallyPointCommandType rally = new SetRallyPointCommandType(4);
 * sink.submit(player, rally, rally.create(game, producer, x, y));
 *}
 * <p>
 * The payload is {@value #PAYLOAD_LENGTH} little-endian bytes: the producer handle (four bytes, {@link EntityPool#createHandle}) and the rally
 * point's x and y (four bytes each). Executing a command resolves the handle against the game's unit pool: a unit whose slot was freed or
 * reused since, a unit the issuing player does not own, or a game without the {@link StandardProductionSystem} does nothing. Otherwise the
 * production system sets the rally point ({@link StandardProductionSystem#setRallyPoint}).
 * <p>
 * Determinism contract: executing a command changes only the producer's rally point, through the game's production system.
 */
public final class SetRallyPointCommandType implements CommandType<SetRallyPointCommand> {
    /**
     * The length of the encoded payload in bytes.
     */
    public static final int PAYLOAD_LENGTH = 12;

    private final int id;

    /**
     * Creates a {@link SetRallyPointCommandType} instance.
     *
     * @param id the id of the type in the mod's command stream, {@code 0} or more
     * @throws IllegalArgumentException if the id is negative
     */
    public SetRallyPointCommandType(int id) {
        if (id < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid command id: {0}", id);
        }

        this.id = id;
    }

    /**
     * @return the id of the type in the mod's command stream, {@code 0} or more
     */
    @Override
    public int getId() {
        return this.id;
    }


    /**
     * Creates a {@link SetRallyPointCommand} instance that sets the rally point of a producer, encoding the producer for the game.
     *
     * @param game     the game the producer belongs to
     * @param producer the producing unit
     * @param x        the x coordinate of the rally point in pixels
     * @param y        the y coordinate of the rally point in pixels
     * @return the created {@link SetRallyPointCommand} instance
     */
    public @NotNull SetRallyPointCommand create(@NotNull Game game, @NotNull Unit producer, int x, int y) {
        return new SetRallyPointCommand(game.getUnits().createHandle(producer), x, y);
    }

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @Override
    public @NotNull SetRallyPointCommand read(@NotNull CommandInput input) throws IOException {
        int producerHandle = input.readInt();
        int x = input.readInt();
        int y = input.readInt();

        return new SetRallyPointCommand(producerHandle, x, y);
    }

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    @Override
    public void write(@NotNull SetRallyPointCommand command, @NotNull CommandOutput output) {
        output.writeInt(command.getProducerHandle())
              .writeInt(command.getX())
              .writeInt(command.getY());
    }

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    @Override
    public void execute(@NotNull Game game, @NotNull Player player, @NotNull SetRallyPointCommand command) {
        Unit producer = game.getUnits().resolveHandle(command.getProducerHandle());

        if (producer == null || producer.getOwner() != player || !game.hasSystem(ProductionSystem.KEY) ||
            !(game.getProduction() instanceof StandardProductionSystem production)) {
            return;
        }

        production.setRallyPoint(producer, command.getX(), command.getY());
    }
}
