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
 * Cancels what a player's producing unit makes: one entry of its queue ({@link ProductionSystem#cancel}), or, for a building under
 * construction, its construction ({@link ProductionSystem#cancelConstruction}). It is the engine default command of a person's cancel; a mod
 * registers it in its {@link org.fogofwarlabs.sc4j.engine.command.CommandRegistry} under the id of its command stream.
 * {@snippet :
 * CancelProductionCommandType cancel = new CancelProductionCommandType(3, instructionLimit);
 * sink.submit(player, cancel, cancel.create(game, producer, slot));
 *}
 * <p>
 * The payload is {@value #PAYLOAD_LENGTH} little-endian bytes: the producer handle (four bytes, {@link EntityPool#createHandle}) and the queue
 * slot (one byte). Executing a command resolves the handle against the game's unit pool: a unit whose slot was freed or reused since or a unit
 * the issuing player does not own does nothing. A building under construction cancels its construction, whatever the slot; any other unit
 * cancels the entry in the slot, and an empty slot does nothing. The refunds are the production system's.
 * <p>
 * Determinism contract: executing a command changes only the producer's queue or construction and the owner's resources and supply, through
 * the game's production system.
 */
public final class CancelProductionCommandType implements CommandType<CancelProductionCommand> {
    /**
     * The length of the encoded payload in bytes.
     */
    public static final int PAYLOAD_LENGTH = 5;
    /**
     * The largest queue slot a command names.
     */
    public static final int MAXIMUM_SLOT = 0xFF;

    private final int id;
    private final int instructionLimit;

    /**
     * Creates a {@link CancelProductionCommandType} instance.
     *
     * @param id               the id of the type in the mod's command stream, {@code 0} or more
     * @param instructionLimit the instruction limit of each animation program run the cancellation starts
     * @throws IllegalArgumentException if the id is negative
     */
    public CancelProductionCommandType(int id, int instructionLimit) {
        if (id < 0) {
            throw Exceptions.newIllegalArgumentException("Invalid command id: {0}", id);
        }

        this.id               = id;
        this.instructionLimit = instructionLimit;
    }

    /**
     * @return the id of the type in the mod's command stream, {@code 0} or more
     */
    @Override
    public int getId() {
        return this.id;
    }


    /**
     * Creates a {@link CancelProductionCommand} instance that cancels a queue slot of a producer, or the construction of a building under
     * construction, encoding the producer for the game.
     *
     * @param game     the game the producer belongs to
     * @param producer the producing unit, or the building under construction
     * @param slot     the queue slot, {@code 0} up to {@link #MAXIMUM_SLOT}; {@code 0} for a building under construction
     * @return the created {@link CancelProductionCommand} instance
     * @throws IllegalArgumentException if the slot is outside {@code 0} to {@link #MAXIMUM_SLOT}
     */
    public @NotNull CancelProductionCommand create(@NotNull Game game, @NotNull Unit producer, int slot) {
        if (slot < 0 || slot > MAXIMUM_SLOT) {
            throw Exceptions.newIllegalArgumentException("Invalid queue slot: {0}", slot);
        }

        return new CancelProductionCommand(game.getUnits().createHandle(producer), slot);
    }

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @Override
    public @NotNull CancelProductionCommand read(@NotNull CommandInput input) throws IOException {
        int producerHandle = input.readInt();
        int slot = input.readUnsignedByte();

        return new CancelProductionCommand(producerHandle, slot);
    }

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    @Override
    public void write(@NotNull CancelProductionCommand command, @NotNull CommandOutput output) {
        output.writeInt(command.getProducerHandle())
              .writeByte(command.getSlot());
    }

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    @Override
    public void execute(@NotNull Game game, @NotNull Player player, @NotNull CancelProductionCommand command) {
        Unit producer = game.getUnits().resolveHandle(command.getProducerHandle());

        if (producer == null || producer.getOwner() != player || !game.hasSystem(ProductionSystem.KEY)) {
            return;
        }

        ProductionSystem production = game.getProduction();

        if (production.isUnderConstruction(producer)) {
            production.cancelConstruction(producer, this.instructionLimit);
        } else {
            production.cancel(producer, command.getSlot(), this.instructionLimit);
        }
    }
}
