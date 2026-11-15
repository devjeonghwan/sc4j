package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Queues a unit type at a player's producing unit: the engine default command of a person's training, the queue entry the computer player
 * makes directly. A mod registers it in its {@link org.fogofwarlabs.sc4j.engine.command.CommandRegistry} under the id of its command stream.
 * {@snippet :
 * TrainUnitCommandType train = new TrainUnitCommandType(1);
 * sink.submit(player, train, train.create(game, producer, type));
 *}
 * <p>
 * The payload is {@value #PAYLOAD_LENGTH} little-endian bytes: the producer handle (four bytes, {@link EntityPool#createHandle}) and the unit
 * type index (two bytes). Executing a command resolves the handle against the game's unit pool: a unit whose slot was freed or reused since, a
 * unit the issuing player does not own or a unit type index outside the content does nothing. Otherwise the production system queues the type
 * ({@link ProductionSystem#enqueueUnit}), which refuses what the requirements do not allow, a full queue or a cost the player cannot pay.
 * <p>
 * Determinism contract: executing a command changes only the producer's queue and the owner's resources, through the game's production
 * system.
 */
public final class TrainUnitCommandType implements CommandType<TrainUnitCommand> {
    /**
     * The length of the encoded payload in bytes.
     */
    public static final int PAYLOAD_LENGTH = 6;

    private final int id;

    /**
     * Creates a {@link TrainUnitCommandType} instance.
     *
     * @param id the id of the type in the mod's command stream, {@code 0} or more
     * @throws IllegalArgumentException if the id is negative
     */
    public TrainUnitCommandType(int id) {
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
     * Creates a {@link TrainUnitCommand} instance that queues a unit type at a producer, encoding both for the game.
     *
     * @param game     the game the producer belongs to
     * @param producer the producing unit
     * @param type     the unit type to train
     * @return the created {@link TrainUnitCommand} instance
     */
    public @NotNull TrainUnitCommand create(@NotNull Game game, @NotNull Unit producer, @NotNull UnitType type) {
        return new TrainUnitCommand(game.getUnits().createHandle(producer), type.getIndex());
    }

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @Override
    public @NotNull TrainUnitCommand read(@NotNull CommandInput input) throws IOException {
        int producerHandle = input.readInt();
        int unitTypeIndex = input.readUnsignedShort();

        return new TrainUnitCommand(producerHandle, unitTypeIndex);
    }

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    @Override
    public void write(@NotNull TrainUnitCommand command, @NotNull CommandOutput output) {
        output.writeInt(command.getProducerHandle())
              .writeShort(command.getUnitTypeIndex());
    }

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    @Override
    public void execute(@NotNull Game game, @NotNull Player player, @NotNull TrainUnitCommand command) {
        EntityPool<Unit> units = game.getUnits();
        TypeRegistry<UnitType> unitTypes = game.getContent().getDefinition().getUnitTypes().getTypes();
        Unit producer = units.resolveHandle(command.getProducerHandle());

        if (producer == null || producer.getOwner() != player || command.getUnitTypeIndex() >= unitTypes.getCount() ||
            !game.hasSystem(ProductionSystem.KEY)) {
            return;
        }

        game.getProduction().enqueueUnit(producer, unitTypes.get(command.getUnitTypeIndex()));
    }
}
