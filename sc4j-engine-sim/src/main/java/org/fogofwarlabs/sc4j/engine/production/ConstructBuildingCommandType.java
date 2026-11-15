package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Orders a player's worker to construct a building at a site: the engine default command of a person's construction, the order the
 * computer player gives directly. A mod registers it in its {@link org.fogofwarlabs.sc4j.engine.command.CommandRegistry} under the id of its
 * command stream.
 * {@snippet :
 * ConstructBuildingCommandType construct = new ConstructBuildingCommandType(2, instructionLimit);
 * sink.submit(player, construct, construct.create(game, worker, type, siteX, siteY));
 *}
 * <p>
 * The payload is {@value #PAYLOAD_LENGTH} little-endian bytes: the worker handle (four bytes, {@link EntityPool#createHandle}), the building
 * type index (two bytes) and the site's center (four bytes each). Executing a command resolves the handle against the game's unit pool: a unit
 * whose slot was freed or reused since, a unit the issuing player does not own, a type index outside the content, a type that is no building, or
 * a building the requirements do not allow this worker to build does nothing. Otherwise the production system orders the construction
 * ({@link ProductionSystem#orderConstruction}); the cost is paid when the building is placed.
 * <p>
 * Determinism contract: executing a command changes only the worker's orders and construction state, through the game's production system.
 */
public final class ConstructBuildingCommandType implements CommandType<ConstructBuildingCommand> {
    /**
     * The length of the encoded payload in bytes.
     */
    public static final int PAYLOAD_LENGTH = 14;

    private final int id;
    private final int instructionLimit;

    /**
     * Creates a {@link ConstructBuildingCommandType} instance.
     *
     * @param id               the id of the type in the mod's command stream, {@code 0} or more
     * @param instructionLimit the instruction limit of each animation program run the order starts
     * @throws IllegalArgumentException if the id is negative
     */
    public ConstructBuildingCommandType(int id, int instructionLimit) {
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
     * Creates a {@link ConstructBuildingCommand} instance that orders a worker to construct a building, encoding both for the game.
     *
     * @param game   the game the worker belongs to
     * @param worker the worker
     * @param type   the building type
     * @param siteX  the x coordinate of the site's center in pixels
     * @param siteY  the y coordinate of the site's center in pixels
     * @return the created {@link ConstructBuildingCommand} instance
     */
    public @NotNull ConstructBuildingCommand create(@NotNull Game game, @NotNull Unit worker, @NotNull UnitType type, int siteX, int siteY) {
        return new ConstructBuildingCommand(game.getUnits().createHandle(worker), type.getIndex(), siteX, siteY);
    }

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @Override
    public @NotNull ConstructBuildingCommand read(@NotNull CommandInput input) throws IOException {
        int workerHandle = input.readInt();
        int buildingTypeIndex = input.readUnsignedShort();
        int siteX = input.readInt();
        int siteY = input.readInt();

        return new ConstructBuildingCommand(workerHandle, buildingTypeIndex, siteX, siteY);
    }

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    @Override
    public void write(@NotNull ConstructBuildingCommand command, @NotNull CommandOutput output) {
        output.writeInt(command.getWorkerHandle())
              .writeShort(command.getBuildingTypeIndex())
              .writeInt(command.getSiteX())
              .writeInt(command.getSiteY());
    }

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    @Override
    public void execute(@NotNull Game game, @NotNull Player player, @NotNull ConstructBuildingCommand command) {
        EntityPool<Unit> units = game.getUnits();
        TypeRegistry<UnitType> unitTypes = game.getContent().getDefinition().getUnitTypes().getTypes();
        Unit worker = units.resolveHandle(command.getWorkerHandle());

        if (worker == null || worker.getOwner() != player || command.getBuildingTypeIndex() >= unitTypes.getCount() ||
            !game.hasSystem(ProductionSystem.KEY)) {
            return;
        }

        UnitType type = unitTypes.get(command.getBuildingTypeIndex());

        if (!game.getContent().getUnitTypes().hasTrait(type, UnitTrait.BUILDING) ||
            game.hasSystem(RequirementSystem.KEY) && !game.getRequirements().check(RequirementAction.BUILD_UNIT, type, player, worker).isMet()) {
            return;
        }

        game.getProduction().orderConstruction(worker, type, command.getSiteX(), command.getSiteY(), this.instructionLimit);
    }
}
