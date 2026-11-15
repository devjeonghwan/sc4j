package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default order system in an engine-only game: issued orders become current with their target, queued orders follow in
 * order from a bounded pool, a finished order with an empty queue becomes the idle order of the owner's controller kind, and the update runs
 * the behavior of the current order type.
 */
final class StandardOrderSystemTest {
    private static final int UNIT_LIMIT = 4;
    private static final int ORDER_LIMIT = 2;
    private static final int INSTRUCTION_LIMIT = 7;

    private static final int HUMAN_PLAYER = 0;
    private static final int COMPUTER_PLAYER = 1;

    private final @NotNull OrderType move;
    private final @NotNull OrderType stop;
    private final @NotNull OrderType guard;
    private final @NotNull OrderType hunt;
    private final @NotNull UnitType soldier;

    private int moveUpdates;
    private int lastInstructionLimit;

    StandardOrderSystemTest() {
        this.move    = new OrderType("move");
        this.stop    = new OrderType("stop");
        this.guard   = new OrderType("guard");
        this.hunt    = new OrderType("hunt");
        this.soldier = new UnitType("soldier");

        this.moveUpdates          = 0;
        this.lastInstructionLimit = 0;
    }

    @Test
    void issued_orders_replace_the_queue_and_become_current() {
        Game game = this.createGame();
        OrderSystem orders = game.getOrders();
        Unit unit = this.createUnit(game, HUMAN_PLAYER);
        Unit target = this.createUnit(game, COMPUTER_PLAYER);

        Assertions.assertTrue(orders.queue(unit, this.stop, 1, 2, null));

        orders.issue(unit, this.move, 30, 40, target, INSTRUCTION_LIMIT);

        Assertions.assertSame(this.move, unit.getOrderType());
        Assertions.assertEquals(30, unit.getOrderTargetX());
        Assertions.assertEquals(40, unit.getOrderTargetY());
        Assertions.assertSame(target, unit.getOrderTargetUnit());
        Assertions.assertNull(unit.getFirstOrder());
        Assertions.assertNull(unit.getLastOrder());
        Assertions.assertEquals(ORDER_LIMIT, ((StandardOrderSystem) orders).getFreeOrderCount());
    }

    @Test
    void queued_orders_follow_in_order_until_the_pool_is_empty() {
        Game game = this.createGame();
        OrderSystem orders = game.getOrders();
        Unit unit = this.createUnit(game, HUMAN_PLAYER);

        orders.issue(unit, this.move, 0, 0, null, INSTRUCTION_LIMIT);

        Assertions.assertTrue(orders.queue(unit, this.stop, 5, 6, null));
        Assertions.assertTrue(orders.queue(unit, this.move, 7, 8, null));
        Assertions.assertFalse(orders.queue(unit, this.stop, 9, 10, null));

        orders.finishOrder(unit, INSTRUCTION_LIMIT);

        Assertions.assertSame(this.stop, unit.getOrderType());
        Assertions.assertEquals(5, unit.getOrderTargetX());
        Assertions.assertEquals(6, unit.getOrderTargetY());
        Assertions.assertTrue(orders.queue(unit, this.stop, 9, 10, null));

        orders.finishOrder(unit, INSTRUCTION_LIMIT);

        Assertions.assertSame(this.move, unit.getOrderType());
        Assertions.assertEquals(7, unit.getOrderTargetX());

        orders.finishOrder(unit, INSTRUCTION_LIMIT);

        Assertions.assertSame(this.stop, unit.getOrderType());
        Assertions.assertEquals(9, unit.getOrderTargetX());
        Assertions.assertNull(unit.getFirstOrder());
        Assertions.assertEquals(ORDER_LIMIT, ((StandardOrderSystem) orders).getFreeOrderCount());
    }

    @Test
    void finished_orders_return_to_the_idle_order_of_the_owner_controller_kind() {
        Game game = this.createGame();
        OrderSystem orders = game.getOrders();
        Unit human = this.createUnit(game, HUMAN_PLAYER);
        Unit computer = this.createUnit(game, COMPUTER_PLAYER);

        human.setPosition(11, 12);
        orders.issue(human, this.move, 50, 50, computer, INSTRUCTION_LIMIT);
        orders.finishOrder(human, INSTRUCTION_LIMIT);
        orders.finishOrder(computer, INSTRUCTION_LIMIT);

        Assertions.assertSame(this.guard, human.getOrderType());
        Assertions.assertEquals(11, human.getOrderTargetX());
        Assertions.assertEquals(12, human.getOrderTargetY());
        Assertions.assertNull(human.getOrderTargetUnit());
        Assertions.assertSame(this.hunt, computer.getOrderType());
    }

    @Test
    void the_update_runs_the_behavior_of_the_current_order_type() {
        Game game = this.createGame();
        OrderSystem orders = game.getOrders();
        Unit unit = this.createUnit(game, HUMAN_PLAYER);

        orders.update(unit, INSTRUCTION_LIMIT);
        orders.issue(unit, this.stop, 0, 0, null, INSTRUCTION_LIMIT);
        orders.update(unit, INSTRUCTION_LIMIT);

        Assertions.assertEquals(0, this.moveUpdates);

        orders.issue(unit, this.move, 0, 0, null, INSTRUCTION_LIMIT);
        orders.update(unit, INSTRUCTION_LIMIT);
        orders.update(unit, INSTRUCTION_LIMIT);

        Assertions.assertEquals(2, this.moveUpdates);
        Assertions.assertEquals(INSTRUCTION_LIMIT, this.lastInstructionLimit);
        Assertions.assertSame(this.stop, unit.getOrderType());
    }

    private @NotNull Unit createUnit(@NotNull Game game, int playerIndex) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(playerIndex));
        unit.setType(this.soldier);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors));

        builder.getContent().getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.stop, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.hunt, new OrderDefinition.Builder().build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setIdleOrder(ControllerKind.HUMAN, this.guard)
                                                                                               .setIdleOrder(ControllerKind.COMPUTER, this.hunt)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(HUMAN_PLAYER, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(COMPUTER_PLAYER, ControllerKind.COMPUTER, 1, 1, null))
                .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.move, (context, unit) -> {
            this.moveUpdates++;
            this.lastInstructionLimit = context.getInstructionLimit();

            if (this.moveUpdates == 2) {
                context.getGame().getOrders().issue(unit, this.stop, 0, 0, null, context.getInstructionLimit());
            }
        });
    }
}
