package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.TypeRegistry;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Issues or queues one order of a player's unit: the engine default command that carries a player's orders into the simulation. A mod
 * registers it in its {@link org.fogofwarlabs.sc4j.engine.command.CommandRegistry} under the id of its command stream.
 * {@snippet :
 * IssueOrderCommandType orders = new IssueOrderCommandType(0, instructionLimit);
 * sink.submit(player, orders, orders.create(game, unit, move, x, y, null, false));
 *}
 * <p>
 * The payload is {@value #PAYLOAD_LENGTH} little-endian bytes: the unit handle (four bytes, {@link EntityPool#createHandle}), the order type index
 * (two bytes), the target point (four bytes each), the target unit handle (four bytes, {@link #NO_UNIT} for none) and the queued flag (one byte,
 * {@code 0} or {@code 1}). Executing a command resolves the handles against the game's unit pool: a unit whose slot was freed or reused since,
 * a unit the issuing player does not own or an order type index outside the content does nothing; a stale target unit handle becomes no target
 * unit. A queued order is appended ({@link OrderSystem#queue}), any other replaces the unit's orders ({@link OrderSystem#issue}).
 * <p>
 * Determinism contract: executing a command changes only the ordered unit's orders, through the game's order system.
 */
public final class IssueOrderCommandType implements CommandType<IssueOrderCommand> {
    /**
     * The target unit handle of a command without target unit.
     */
    public static final int NO_UNIT = 0;
    /**
     * The length of the encoded payload in bytes.
     */
    public static final int PAYLOAD_LENGTH = 19;

    private final int id;
    private final int instructionLimit;

    /**
     * Creates an {@link IssueOrderCommandType} instance.
     *
     * @param id               the id of the type in the mod's command stream, {@code 0} or more
     * @param instructionLimit the instruction limit of each animation program run an issued order starts
     * @throws IllegalArgumentException if the id is negative
     */
    public IssueOrderCommandType(int id, int instructionLimit) {
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
     * Creates an {@link IssueOrderCommand} instance that orders a unit, encoding the unit, the order type and the target unit for the game.
     *
     * @param game       the game the unit belongs to
     * @param unit       the ordered unit
     * @param orderType  the order type
     * @param targetX    the x coordinate of the target point
     * @param targetY    the y coordinate of the target point
     * @param targetUnit the target unit, or {@code null} for none
     * @param queued     whether the order is queued after the unit's orders
     * @return the created {@link IssueOrderCommand} instance
     */
    public @NotNull IssueOrderCommand create(@NotNull Game game, @NotNull Unit unit, @NotNull OrderType orderType, int targetX, int targetY,
                                             @Nullable Unit targetUnit, boolean queued) {
        EntityPool<Unit> units = game.getUnits();

        return new IssueOrderCommand(units.createHandle(unit), orderType.getIndex(), targetX, targetY,
                                     targetUnit != null ? units.createHandle(targetUnit) : NO_UNIT, queued);
    }

    /**
     * Reads a command from its encoded payload.
     *
     * @param input the payload of one command, positioned at its first byte
     * @return the command
     * @throws IOException if the payload is not a command of this type
     */
    @Override
    public @NotNull IssueOrderCommand read(@NotNull CommandInput input) throws IOException {
        int unitHandle = input.readInt();
        int orderTypeIndex = input.readUnsignedShort();
        int targetX = input.readInt();
        int targetY = input.readInt();
        int targetUnitHandle = input.readInt();
        int queued = input.readUnsignedByte();

        if (queued > 1) {
            throw Exceptions.newIOException("Invalid queued flag of an order command: {0}", queued);
        }

        return new IssueOrderCommand(unitHandle, orderTypeIndex, targetX, targetY, targetUnitHandle, queued == 1);
    }

    /**
     * Writes the encoded payload of a command.
     *
     * @param command the command
     * @param output  the output the payload is appended to
     */
    @Override
    public void write(@NotNull IssueOrderCommand command, @NotNull CommandOutput output) {
        output.writeInt(command.getUnitHandle())
              .writeShort(command.getOrderTypeIndex())
              .writeInt(command.getTargetX())
              .writeInt(command.getTargetY())
              .writeInt(command.getTargetUnitHandle())
              .writeByte(command.isQueued() ? 1 : 0);
    }

    /**
     * Applies a command of a player to the game.
     *
     * @param game    the game
     * @param player  the issuing player
     * @param command the command
     */
    @Override
    public void execute(@NotNull Game game, @NotNull Player player, @NotNull IssueOrderCommand command) {
        EntityPool<Unit> units = game.getUnits();
        TypeRegistry<OrderType> orderTypes = game.getContent().getDefinition().getOrderTypes().getTypes();
        Unit unit = units.resolveHandle(command.getUnitHandle());

        if (unit == null || unit.getOwner() != player || command.getOrderTypeIndex() >= orderTypes.getCount()) {
            return;
        }

        OrderType orderType = orderTypes.get(command.getOrderTypeIndex());
        Unit targetUnit = command.getTargetUnitHandle() != NO_UNIT ? units.resolveHandle(command.getTargetUnitHandle()) : null;

        if (command.isQueued()) {
            game.getOrders().queue(unit, orderType, command.getTargetX(), command.getTargetY(), targetUnit);
        } else {
            game.getOrders().issue(unit, orderType, command.getTargetX(), command.getTargetY(), targetUnit, this.instructionLimit);
        }
    }
}
