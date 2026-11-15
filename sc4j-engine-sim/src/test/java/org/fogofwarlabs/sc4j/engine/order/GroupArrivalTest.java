package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.collision.CollisionSystem;
import org.fogofwarlabs.sc4j.engine.collision.StandardCollisionSystem;
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
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the group arrival of the move order: a group sent to one point stops around it instead of pushing for the point, and a unit far from
 * the point does not stop at a standing unit of its group.
 */
final class GroupArrivalTest {
    private static final int UNIT_LIMIT = 16;
    private static final int ORDER_LIMIT = 32;
    private static final int GROUP_SIZE = 9;
    private static final int UNIT_EXTENT = 6;
    private static final int ARRIVAL_DISTANCE = 4;
    private static final int GROUP_DISTANCE = 96;
    private static final int SEARCH_DISTANCE = 64;
    private static final int FRAME_LIMIT = 600;
    private static final int START_X = 100;
    private static final int START_Y = 100;
    private static final int SPACING = 16;
    private static final int TARGET_X = 400;
    private static final int TARGET_Y = 300;

    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType move;
    private final @NotNull UnitType soldier;
    private final @NotNull TableUnitBounds bounds;

    GroupArrivalTest() {
        this.walker  = new MotionProfile("walker");
        this.idle    = new OrderType("idle");
        this.move    = new OrderType("move");
        this.soldier = new UnitType("soldier");
        this.bounds  = new TableUnitBounds();
    }

    @Test
    void a_group_sent_to_one_point_stops_around_it() {
        Game game = this.createGame();
        List<Unit> group = new ArrayList<>();

        for (int index = 0; index < GROUP_SIZE; index++) {
            group.add(this.createUnit(game, START_X + index % 3 * SPACING, START_Y + index / 3 * SPACING));
        }

        for (Unit unit : group) {
            game.getOrders().issue(unit, this.move, TARGET_X, TARGET_Y, null, 0);
        }

        int frame = 0;

        while (frame < FRAME_LIMIT && group.stream().anyMatch(unit -> unit.getOrderType() != this.idle)) {
            game.getFrameRunner().runFrame();
            frame++;
        }

        Assertions.assertTrue(frame < FRAME_LIMIT, "every unit of the group arrived");

        for (Unit unit : group) {
            Assertions.assertTrue(Math.abs(unit.getPositionX() - TARGET_X) <= GROUP_DISTANCE &&
                                  Math.abs(unit.getPositionY() - TARGET_Y) <= GROUP_DISTANCE,
                                  "the group stands around the point");
        }
    }

    @Test
    void has_arrived_should_need_a_standing_unit_nearer_to_the_point_within_the_distance() {
        Game game = this.createGame();
        GroupArrival arrival = new GroupArrival(this.bounds, GROUP_DISTANCE);
        Unit standing = this.createUnit(game, TARGET_X, TARGET_Y);
        Unit touching = this.createUnit(game, TARGET_X + 2 * UNIT_EXTENT + 1, TARGET_Y);
        Unit apart = this.createUnit(game, TARGET_X - 4 * UNIT_EXTENT, TARGET_Y);

        Assertions.assertTrue(arrival.hasArrived(game, touching, TARGET_X, TARGET_Y));
        Assertions.assertFalse(arrival.hasArrived(game, standing, TARGET_X, TARGET_Y), "no standing unit is nearer to the point");
        Assertions.assertFalse(arrival.hasArrived(game, apart, TARGET_X, TARGET_Y), "the unit touches no standing unit");
        Assertions.assertFalse(arrival.hasArrived(game, touching, TARGET_X + 2 * GROUP_DISTANCE, TARGET_Y), "the point is beyond the distance");
        Assertions.assertEquals(GROUP_DISTANCE, arrival.getDistance());
    }

    private @NotNull Unit createUnit(@NotNull Game game, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(0), this.soldier, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        int largestSize = 2 * UNIT_EXTENT + 1;
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(this.bounds, largestSize, largestSize))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(CollisionSystem.KEY, context -> new StandardCollisionSystem(this.bounds, SEARCH_DISTANCE))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(GroupArrivalTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement)
                .addFramePhase(CollisionSystem.KEY, collision -> (StandardCollisionSystem) collision);

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(320)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(1024)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .setCollisionLeft(UNIT_EXTENT)
                                                                                               .setCollisionUp(UNIT_EXTENT)
                                                                                               .setCollisionRight(UNIT_EXTENT)
                                                                                               .setCollisionDown(UNIT_EXTENT)
                                                                                               .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                                               .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.move, new MoveBehavior(ARRIVAL_DISTANCE, new GroupArrival(this.bounds, GROUP_DISTANCE)));
    }

    private static void updateOrders(@NotNull Game game) {
        SlotList used = game.getUnits().getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(game.getUnits().get(index), 0);
        }
    }
}
