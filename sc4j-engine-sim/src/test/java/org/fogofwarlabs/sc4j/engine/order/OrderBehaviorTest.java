package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default move and stop behaviors and the order start of the standard order system in an engine-only game: issued and
 * finished orders start their behavior, a moved unit ends its order within the arrival distance and idles, a queued move follows, and a stop
 * order stops a moving unit.
 */
final class OrderBehaviorTest {
    private static final int UNIT_LIMIT = 4;
    private static final int ORDER_LIMIT = 4;
    private static final int ARRIVAL_DISTANCE = 4;
    private static final int FRAME_LIMIT = 400;

    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType move;
    private final @NotNull OrderType stop;
    private final @NotNull UnitType soldier;
    private final @NotNull List<@NotNull String> starts;

    OrderBehaviorTest() {
        this.walker  = new MotionProfile("walker");
        this.idle    = new OrderType("idle");
        this.move    = new OrderType("move");
        this.stop    = new OrderType("stop");
        this.soldier = new UnitType("soldier");
        this.starts  = new ArrayList<>();
    }

    @Test
    void moved_units_end_their_order_within_the_arrival_distance_and_idle() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 20, 20);

        Assertions.assertEquals(List.of("idle"), this.starts);

        game.getOrders().issue(unit, this.move, 150, 60, null, 0);
        game.getOrders().queue(unit, this.move, 150, 140, null);

        Assertions.assertEquals(List.of("idle"), this.starts);
        Assertions.assertEquals(150, unit.getMovementDestinationX());
        Assertions.assertEquals(60, unit.getMovementDestinationY());

        this.runUntilIdle(game, unit);

        Assertions.assertSame(this.idle, unit.getOrderType());
        Assertions.assertEquals(List.of("idle", "idle"), this.starts);
        Assertions.assertTrue(Math.abs(unit.getPositionX() - 150) <= ARRIVAL_DISTANCE);
        Assertions.assertTrue(Math.abs(unit.getPositionY() - 140) <= ARRIVAL_DISTANCE);
        Assertions.assertTrue(game.getMovement().isAtDestination(unit));
    }

    @Test
    void stop_orders_stop_moving_units() {
        Game game = this.createGame();
        Unit unit = this.createUnit(game, 20, 20);

        game.getOrders().issue(unit, this.move, 300, 20, null, 0);
        this.runFrames(game, 20);

        Assertions.assertTrue(unit.getCurrentSpeed() > 0);

        game.getOrders().issue(unit, this.stop, 0, 0, null, 0);
        game.getFrameRunner().runFrame();

        int stoppedX = unit.getPositionX();

        this.runFrames(game, 20);

        Assertions.assertSame(this.idle, unit.getOrderType());
        Assertions.assertEquals(stoppedX, unit.getPositionX());
        Assertions.assertTrue(unit.getPositionX() < 300);
    }

    private void runUntilIdle(@NotNull Game game, @NotNull Unit unit) {
        for (int frame = 0; frame < FRAME_LIMIT && unit.getOrderType() != this.idle; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Unit createUnit(@NotNull Game game, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(0), this.soldier, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(OrderBehaviorTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.stop, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.idle, new OrderBehavior<>() {
            @Override
            public void start(@NotNull OrderContext context, @NotNull Unit unit) {
                OrderBehaviorTest.this.starts.add("idle");
            }

            @Override
            public void update(@NotNull OrderContext context, @NotNull Unit unit) {
            }
        });
        behaviors.register(this.move, new MoveBehavior(ARRIVAL_DISTANCE));
        behaviors.register(this.stop, new StopBehavior());
    }

    private static void updateOrders(@NotNull Game game) {
        SlotList used = game.getUnits().getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(game.getUnits().get(index), 0);
        }
    }
}
