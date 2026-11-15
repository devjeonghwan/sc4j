package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default follow behavior over the standard order and movement systems: a follower moves after its target and stops within the
 * follow range, a follow target hook takes a target it uses, and a follow without target ends in the idle order.
 */
final class FollowBehaviorTest {
    private static final int UNIT_LIMIT = 4;
    private static final int ORDER_LIMIT = 4;
    private static final int FOLLOW_RANGE = 16;
    private static final int FRAME_LIMIT = 200;
    private static final int INSTRUCTION_LIMIT = 0;

    private static final int TOP_SPEED = 512;
    private static final int ACCELERATION = 64;
    private static final int STOPPING_DISTANCE = 2048;
    private static final int TURN_RATE = 40;

    private final @NotNull OrderType follow;
    private final @NotNull OrderType guard;
    private final @NotNull OrderType enter;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType transport;
    private final @NotNull MotionProfile walker;

    FollowBehaviorTest() {
        this.follow    = new OrderType("follow");
        this.guard     = new OrderType("guard");
        this.enter     = new OrderType("enter");
        this.soldier   = new UnitType("soldier");
        this.transport = new UnitType("transport");
        this.walker    = new MotionProfile("walker");
    }

    @Test
    void followers_move_after_their_target_and_stop_in_range() {
        Game game = this.createGame();
        Unit target = this.createUnit(game, this.soldier, 200, 40);
        Unit follower = this.createUnit(game, this.soldier, 20, 40);

        game.getOrders().issue(follower, this.follow, 0, 0, target, INSTRUCTION_LIMIT);

        for (int frame = 0; frame < FRAME_LIMIT; frame++) {
            game.getFrameRunner().runFrame();
        }

        Assertions.assertSame(this.follow, follower.getOrderType());
        Assertions.assertTrue(follower.getPositionX() > 20);
        Assertions.assertTrue(Math.abs(target.getPositionX() - follower.getPositionX()) <= FOLLOW_RANGE);
        Assertions.assertEquals(follower.getPositionX(), follower.getMovementDestinationX());
    }

    @Test
    void a_follow_target_hook_takes_a_target_the_unit_uses() {
        Game game = this.createGame();
        Unit target = this.createUnit(game, this.transport, 200, 40);
        Unit follower = this.createUnit(game, this.soldier, 20, 40);

        game.getOrders().issue(follower, this.follow, 0, 0, target, INSTRUCTION_LIMIT);
        game.getFrameRunner().runFrame();

        Assertions.assertSame(this.enter, follower.getOrderType());
        Assertions.assertSame(target, follower.getOrderTargetUnit());
        Assertions.assertEquals(20, follower.getPositionX());
    }

    @Test
    void a_follow_without_target_ends_in_the_idle_order() {
        Game game = this.createGame();
        Unit follower = this.createUnit(game, this.soldier, 20, 40);

        game.getOrders().issue(follower, this.follow, 0, 0, null, INSTRUCTION_LIMIT);
        game.getFrameRunner().runFrame();

        Assertions.assertSame(this.guard, follower.getOrderType());
    }

    private @NotNull Unit createUnit(@NotNull Game game, @NotNull UnitType type, int x, int y) {
        Unit unit = game.getUnits().allocate();

        Assertions.assertNotNull(unit);

        unit.setOwner(game.getPlayer(0));
        unit.setType(type);
        unit.setMotionProfile(this.walker);
        unit.setPosition(x, y);
        unit.setMovementDestinationX(x);
        unit.setMovementDestinationY(y);
        unit.setMovementWaypointX(x);
        unit.setMovementWaypointY(y);
        unit.setDirectionTargetX(x);
        unit.setDirectionTargetY(y);
        game.getMovement().recalculateSpeed(unit, INSTRUCTION_LIMIT);
        game.getOrders().finishOrder(unit, INSTRUCTION_LIMIT);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addHook(OrderSystem.FOLLOW_TARGET, context -> this::enterTransport)
                .addFramePhase(FollowBehaviorTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        builder.getContent().getOrderTypes().register(this.follow, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.enter, new OrderDefinition.Builder().build());

        UnitDefinition idlesInGuard = new UnitDefinition.Builder().setIdleOrder(ControllerKind.HUMAN, this.guard).build();

        builder.getContent().getUnitTypes().register(this.soldier, idlesInGuard);
        builder.getContent().getUnitTypes().register(this.transport, idlesInGuard);
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(TOP_SPEED)
                                                                                                            .setAcceleration(ACCELERATION)
                                                                                                            .setStoppingDistance(STOPPING_DISTANCE)
                                                                                                            .setTurnRate(TURN_RATE)
                                                                                                            .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.follow, new FollowBehavior(game.getHooks(OrderSystem.FOLLOW_TARGET), FOLLOW_RANGE));
    }

    private boolean enterTransport(@NotNull Unit unit, @NotNull Unit target, int instructionLimit) {
        if (target.getType() != this.transport) {
            return false;
        }

        unit.setOrderType(this.enter);

        return true;
    }

    private static void updateOrders(@NotNull Game game) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            game.getOrders().update(units.get(index), INSTRUCTION_LIMIT);
        }
    }
}
