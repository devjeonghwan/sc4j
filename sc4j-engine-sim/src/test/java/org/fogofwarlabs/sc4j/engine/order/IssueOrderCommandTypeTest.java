package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the engine order command: its payload reads back into an equal command, executing it issues or queues the order of the issuing
 * player's unit, and commands for another player's unit, a freed unit or an unknown order type do nothing.
 */
final class IssueOrderCommandTypeTest {
    private static final int COMMAND_ID = 3;

    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType move;
    private final @NotNull UnitType soldier;
    private final @NotNull IssueOrderCommandType commands;

    IssueOrderCommandTypeTest() {
        this.walker   = new MotionProfile("walker");
        this.move     = new OrderType("move");
        this.soldier  = new UnitType("soldier");
        this.commands = new IssueOrderCommandType(COMMAND_ID, 0);
    }

    @Test
    void read_should_return_the_command_that_write_encoded() throws IOException {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 0);
        Unit target = this.createUnit(game, 1);
        IssueOrderCommand command = this.commands.create(game, unit, this.move, -5, 700, target, true);
        CommandOutput output = new CommandOutput();

        this.commands.write(command, output);

        byte[] payload = output.toByteArray();
        IssueOrderCommand read = this.commands.read(new CommandInput(payload));

        Assertions.assertEquals(IssueOrderCommandType.PAYLOAD_LENGTH, payload.length);
        Assertions.assertEquals(COMMAND_ID, this.commands.getId());
        Assertions.assertEquals(command.getUnitHandle(), read.getUnitHandle());
        Assertions.assertEquals(this.move.getIndex(), read.getOrderTypeIndex());
        Assertions.assertEquals(-5, read.getTargetX());
        Assertions.assertEquals(700, read.getTargetY());
        Assertions.assertEquals(game.getUnits().createHandle(target), read.getTargetUnitHandle());
        Assertions.assertTrue(read.isQueued());

        payload[payload.length - 1] = 2;

        Assertions.assertThrows(IOException.class, () -> this.commands.read(new CommandInput(payload)));
    }

    @Test
    void execute_should_order_only_units_of_the_issuing_player() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 0);
        Unit other = this.createUnit(game, 1);

        this.commands.execute(game, game.getPlayer(1), this.commands.create(game, unit, this.move, 90, 90, null, false));

        Assertions.assertNull(unit.getOrderType());

        this.commands.execute(game, game.getPlayer(0), this.commands.create(game, unit, this.move, 90, 90, other, false));

        Assertions.assertSame(this.move, unit.getOrderType());
        Assertions.assertSame(other, unit.getOrderTargetUnit());
        Assertions.assertEquals(90, unit.getMovementDestinationX());

        this.commands.execute(game, game.getPlayer(0), this.commands.create(game, unit, this.move, 40, 40, null, true));

        Assertions.assertNotNull(unit.getFirstOrder());

        IssueOrderCommand stale = this.commands.create(game, other, this.move, 10, 10, null, false);

        other.setOwner(null);
        game.getUnits().free(other);
        this.createUnit(game, 1);
        this.commands.execute(game, game.getPlayer(1), stale);
        this.commands.execute(game, game.getPlayer(0), new IssueOrderCommand(game.getUnits().createHandle(unit), 99, 0, 0,
                                                                             IssueOrderCommandType.NO_UNIT, false));

        Assertions.assertSame(this.move, unit.getOrderType());
        Assertions.assertEquals(90, unit.getOrderTargetX());
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), this.soldier, 20, 20, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(4).setOrders(4).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem((game, behaviors) -> behaviors.register(this.move, new MoveBehavior(0))));

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512).build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
