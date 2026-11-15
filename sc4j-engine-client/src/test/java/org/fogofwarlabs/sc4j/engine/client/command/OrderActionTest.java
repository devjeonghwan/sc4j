package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.client.input.KeyBindingTable;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
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
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine order action: one order command per selected unit, to the pointed target or the unit's own position, executed by the game as
 * the player's orders, and the point order type of an action on a position without a unit.
 */
final class OrderActionTest {
    private static final int COMMAND_ID = 3;
    private static final int CAPACITY = 4;
    private static final int TARGET_X = 90;
    private static final int TARGET_Y = 70;
    private static final int START_X = 20;
    private static final int START_Y = 24;

    /**
     * Executes every submitted order command at once.
     */
    private static final class ExecutingSink implements CommandSink {
        private final @NotNull Game game;

        private int count;

        private ExecutingSink(@NotNull Game game) {
            this.game = game;

            this.count = 0;
        }

        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            type.execute(this.game, player, command);
            this.count++;
        }
    }

    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType move;
    private final @NotNull OrderType attack;
    private final @NotNull OrderType attackMove;
    private final @NotNull UnitType soldier;
    private final @NotNull IssueOrderCommandType commands;

    OrderActionTest() {
        this.walker     = new MotionProfile("walker");
        this.move       = new OrderType("move");
        this.attack     = new OrderType("attack");
        this.attackMove = new OrderType("attack-move");
        this.soldier    = new UnitType("soldier");
        this.commands   = new IssueOrderCommandType(COMMAND_ID, 0);
    }

    @Test
    void issue_should_order_every_selected_unit_of_the_player() {
        Game game = this.createGame();
        Unit first = this.createUnit(game);
        Unit second = this.createUnit(game);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        ExecutingSink sink = new ExecutingSink(game);
        CommandTranslator translator = new CommandTranslator(game.getPlayer(0), sink, selection, new KeyBindingTable<>(), (x, y) -> null);
        OrderAction moveAction = new OrderAction(game, this.commands, this.move, true);

        selection.select(first);
        selection.add(second);
        moveAction.issue(translator, CommandTarget.at(TARGET_X, TARGET_Y), false);

        Assertions.assertEquals(2, sink.count);
        Assertions.assertSame(this.move, first.getOrderType());
        Assertions.assertSame(this.move, second.getOrderType());
        Assertions.assertEquals(TARGET_X, second.getOrderTargetX());
        Assertions.assertEquals(TARGET_Y, second.getOrderTargetY());
        Assertions.assertTrue(moveAction.isTargeted());
        Assertions.assertSame(this.move, moveAction.getOrderType());

        selection.select(first);
        new OrderAction(game, this.commands, this.move, false).issue(translator, null, false);

        Assertions.assertEquals(first.getPositionX(), first.getOrderTargetX(), "an order without a target goes to the unit's position");
        Assertions.assertEquals(TARGET_X, second.getOrderTargetX());
    }

    @Test
    void issue_should_build_commands_that_name_the_units_and_target() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game);
        Unit target = this.createUnit(game);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        IssueOrderCommand[] submitted = new IssueOrderCommand[1];
        CommandSink sink = new CommandSink() {
            @Override
            public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
                submitted[0] = (IssueOrderCommand) command;
            }
        };
        CommandTranslator translator = new CommandTranslator(game.getPlayer(0), sink, selection, new KeyBindingTable<>(), (x, y) -> null);

        selection.select(unit);
        new OrderAction(game, this.commands, this.move, true).issue(translator, CommandTarget.of(target), true);

        Assertions.assertNotNull(submitted[0]);
        Assertions.assertEquals(game.getUnits().createHandle(unit), submitted[0].getUnitHandle());
        Assertions.assertEquals(game.getUnits().createHandle(target), submitted[0].getTargetUnitHandle());
        Assertions.assertEquals(target.getPositionX(), submitted[0].getTargetX());
        Assertions.assertTrue(submitted[0].isQueued());
    }

    @Test
    void issue_should_order_the_point_order_type_on_a_position_without_unit() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game);
        Unit target = this.createUnit(game);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        ExecutingSink sink = new ExecutingSink(game);
        CommandTranslator translator = new CommandTranslator(game.getPlayer(0), sink, selection, new KeyBindingTable<>(), (x, y) -> null);
        OrderAction attackAction = new OrderAction(game, this.commands, this.attack, this.attackMove, true);

        selection.select(unit);
        attackAction.issue(translator, CommandTarget.at(TARGET_X, TARGET_Y), false);

        Assertions.assertSame(this.attackMove, unit.getOrderType(), "an attack on the ground is an attack-move");
        Assertions.assertEquals(TARGET_X, unit.getOrderTargetX());

        attackAction.issue(translator, CommandTarget.of(target), false);

        Assertions.assertSame(this.attack, unit.getOrderType(), "an attack on a unit attacks it");
        Assertions.assertSame(target, unit.getOrderTargetUnit());
        Assertions.assertSame(this.attackMove, attackAction.getPointOrderType());
    }

    private @NotNull Unit createUnit(@NotNull Game game) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(0), this.soldier, START_X, START_Y, 0);

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
        builder.getContent().getOrderTypes().register(this.attack, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.attackMove, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512).build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                .build();

        return Game.create(builder.build(), setup);
    }
}
